#Data : 2018-10-15
#Author : Fengyuan Zhang (Franklin)
#Email : franklinzhang@foxmail.com

from enum import Enum
import socket
import time
import threading
import json
import os
import sys
import datetime
import zipfile

class EModelContextStatus(Enum):
    EMCS_INIT_BEGIN = 1,
    EMCS_INIT = 2,
    EMCS_INIT_END = 3,

    EMCS_STATE_ENTER_BEGIN = 4,
    EMCS_STATE_ENTER = 5,
    EMCS_STATE_ENTER_END = 6,

    EMCS_EVENT_BEGIN = 7,
    EMCS_EVENT = 8,
    EMCS_EVENT_END = 9,

    EMCS_REQUEST_BEGIN = 10,
    EMCS_REQUEST = 11,
    EMCS_REQUEST_END = 12,

    EMCS_RESPONSE_BEGIN = 13,
    EMCS_RESPONSE = 14,
    EMCS_RESPONSE_END = 15,

    EMCS_POST_BEGIN = 16,
    EMCS_POST = 17,
    EMCS_POST_END = 18,

    EMCS_STATE_LEAVE_BEGIN = 19,
    EMCS_STATE_LEAVE = 20,
    EMCS_STATE_LEAVE_END = 21,

    EMCS_FINALIZE_BEGIN = 22,
    EMCS_FINALIZE = 23,
    EMCS_FINALIZE_END = 24,

    EMCS_COMMON_BEGIN = 25,
    EMCS_COMMON_REQUEST = 26,
    EMCS_COMMON_END = 27,

    EMCS_INIT_CTRLPARAM_BEGIN = 28,
    EMCS_INIT_CTRLPARAM = 29,
    EMCS_INIT_CTRLPARAM_END = 30,

    EMCS_UNKOWN = 0

class ERequestResponseDataFlag(Enum):
    ERDF_OK = 1,
    ERDF_NOTREADY = 2,
    ERDF_ERROR = -1,
    ERDF_UNKNOWN = 0

class ERequestResponseDataMIME(Enum):
    ERDM_XML_STREAM = 1,
    ERDM_ZIP_STREAM = 2
    ERDM_RAW_STREAM = 3,
    ERDM_XML_FILE = 4,
    ERDM_ZIP_FILE = 5,
    ERDM_RAW_FILE = 6,
    ERDM_UNKNOWN = 0

class MLog:
    def __init__(self, state = "", event = "", message = "", sr = -1, socketMessage = ""):
        self.datetime = datetime.datetime.now()
        self.state = state
        self.event = event
        self.message = message
        self.sendOrRev = sr   # -1 -> Unknown, 0 -> Received message, 1 -> Sent message
        self.socketMessage = socketMessage

    def export(self):
        srm = ""
        if(self.sendOrRev != -1):
            if(self.sendOrRev == 0):
                srm = "Received socket message: [" + self.socketMessage + "]"
            if(self.sendOrRev == 1):
                srm = "Sent socket message: [" + self.socketMessage + "]"
        return "[" + self.datetime + "] State:[" + self.state + "] Event:[" + self.event + "] " + srm + " Message:[" + self.message + "]"

class ModelServiceContext:
    def __init__(self):
        self.mPort = 6000
        self.mHost = '127.0.0.1'
        self.mInstanceID = ''
        self.mClientSocket = None
        self.mMornitoringThread = None
        self.mDebugScriptFile = ''
        self.mStatus = EModelContextStatus.EMCS_UNKOWN
        self.mData = ''
        self.mMappingLibDir = ''
        self.mInstanceDir = ''
        self.mModelDir = ''
        self.mCurrentState = ''
        self.mCurrentEvent = ''
        self.mRequestDataFlag = ERequestResponseDataFlag.ERDF_UNKNOWN
        self.mRequestDataBody = ''
        self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_UNKNOWN
        
        self.mResponseDataFlag = ERequestResponseDataFlag.ERDF_UNKNOWN
        self.mResponseDataBody = ''
        self.mResponseDataMIME = ERequestResponseDataMIME.ERDM_UNKNOWN

        self.mProcessParams = {}
        self.mControlParams = {}

        self.mLogs = []

    def _bindSocket(self) -> int:
        self.mClientSocket = socket.socket()
        try:
            self.mClientSocket.connect((self.mHost, int(self.mPort)))
        except ZeroDivisionError as ex:
            return -1
        return 1
    
    def _sendMessage(self, message: str) -> None:
        self._pushLog("", 1, message)
        self.mClientSocket.sendall(bytes(message, encoding="utf-8"))

    def _receiveMessage(self) -> str:
        msg = str(self.mClientSocket.recv(10240))
        self._pushLog("", 0, msg)
        return msg

    def _wait4Status(self, status: EModelContextStatus, timeout = 72000) -> int:
        time_end = time.time() + timeout
        while True:
            time.sleep(1)
            if self.mStatus == status or time.time() > time_end :
                return 1
        return -1

    def _resetRequestDataInfo(self) -> None:
        self.mRequestDataBody = ''
        self.mRequestDataFlag = ERequestResponseDataFlag.ERDF_UNKNOWN
        self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_UNKNOWN

    def _resetResponseDataInfo(self) -> None:
        self.mRequestDataBody = ''
        self.mRequestDataFlag = ERequestResponseDataFlag.ERDF_UNKNOWN
        self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_UNKNOWN

    def _sendProcessParam(self) -> None:
        self._sendMessage('{ProcessParams}' + self.mInstanceID + '&' + json.dumps(self.mProcessParams))

    def _pushLog(self, message = "", sr = -1, socketMessage = "") -> None:
        self.mLogs.append(MLog(self.mCurrentState, self.mCurrentEvent, message, sr, socketMessage))

    #! 1 -> windows, 2 -> linux, 3 -> macos, -1 -> Unknown
    def _getOSType(self) -> int:
        pf = sys.platform
        if (pf == "win32"):
            return 1
        elif (pf == "linux"):
            return 2
        elif (pf == "darwin"):
            return 3
        return -1
    
    def _getSlash(self):
        pf = self._getOSType()
        if (pf == 1):
            return "\\"
        else:
            return "/"

    #####! Public interfaces
    #####! Running status functions
    def onInitialize(self, host: str, port: int, instanceID: str) -> None:
        self.mHost = host
        self.mPort = port
        self.mInstanceID = instanceID
        self.mStatus = EModelContextStatus.EMCS_INIT_BEGIN
        if self._bindSocket() == 1:
            # start monitoring thread
            self.mMornitoringThread = threading.Thread(target=ModelServiceContext.Monitoring_thread, name='Monitoring', args=(self,))
            self.mStatus = EModelContextStatus.EMCS_INIT
            if self.mMornitoringThread == None:
                print('Error in creating daemon thread!')
                self._pushLog("Error in creating daemon thread!")
                return exit()
            # fix bug: the sub thread still running after main thread exit (by 7bin)
            self.mMornitoringThread.setDaemon(True)
            self.mMornitoringThread.start()
            self._sendMessage('{init}' + self.mInstanceID + '&' + self.mDebugScriptFile)
            self._wait4Status(EModelContextStatus.EMCS_INIT_END)
            startPos = self.mData.index('[')
            endPos = self.mData.index(']')
            self.mMappingLibDir = self.mData[startPos + 1 : endPos]
            self.mData = self.mData[endPos + 1 : ]
            startPos = self.mData.index('[')
            endPos = self.mData.index(']')
            self.mInstanceDir = self.mData[startPos + 1 : endPos]
            self.onGetModelDirContext()
        else:
            print('Init failed! Cannot connect wrapper system!')
            self._pushLog("Init failed! Cannot connect wrapper system!")
            return exit()
    
    def onEnterState(self, stateId: str) -> None:
        self.mStatus = EModelContextStatus.EMCS_STATE_ENTER_BEGIN
        self.mCurrentState = stateId
        self._pushLog("State enterred!")
        self._sendMessage('{onEnterState}' + self.mInstanceID + '&' + stateId)
        self.mStatus = EModelContextStatus.EMCS_STATE_ENTER
        self._wait4Status(EModelContextStatus.EMCS_STATE_ENTER_END)

    def onFireEvent(self, eventName: str) -> None:
        self.mStatus = EModelContextStatus.EMCS_EVENT_BEGIN
        self.mCurrentEvent = eventName
        self._pushLog("Event fired!")
        self._sendMessage('{onFireEvent}' + self.mInstanceID + "&" + self.mCurrentState + "&" + eventName)
        self.mStatus = EModelContextStatus.EMCS_EVENT
        self._wait4Status(EModelContextStatus.EMCS_EVENT_END)
        
    def onRequestData(self) -> int:
        self._resetRequestDataInfo()
        if self.mCurrentState == '' or self.mCurrentEvent == '':
            return -1
        self._resetRequestDataInfo()
        self.mStatus = EModelContextStatus.EMCS_REQUEST_BEGIN
        self._pushLog("Data requesting start!")
        self._sendMessage('{onRequestData}' + self.mInstanceID + '&' + self.mCurrentState + '&' + self.mCurrentEvent)
        self._wait4Status(EModelContextStatus.EMCS_REQUEST_END)
        
        posBegin = self.mData.index('[')
        posEnd = self.mData.index(']')
        dataFlag = self.mData[posBegin + 1 : posEnd - posBegin]
        dataRemained = self.mData[posEnd + 1 : ]

        if dataFlag == 'OK':
            self.mRequestDataFlag = ERequestResponseDataFlag.ERDF_OK
        elif dataFlag == 'NOTREADY':
            self.mRequestDataFlag = ERequestResponseDataFlag.ERDF_NOTREADY
        else:
            self.mRequestDataFlag = ERequestResponseDataFlag.ERDF_ERROR
            self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_UNKNOWN
            return 0
        
        posBegin = dataRemained.index('[')
        posEnd = dataRemained.index(']')
        dataMIME = dataRemained[posBegin + 1 : posEnd - posBegin]

        if dataMIME == 'XML|STREAM':
            self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_XML_STREAM
        elif dataMIME == 'ZIP|STREAM':
            self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_ZIP_STREAM
        elif dataMIME == 'RAW|STREAM':
            self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_RAW_STREAM
        elif dataMIME == 'XML|FILE':
            self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_XML_FILE
        elif dataMIME == 'ZIP|FILE':
            self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_ZIP_FILE
        elif dataMIME == 'RAW|FILE':
            self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_RAW_FILE
        else:
            self.mRequestDataMIME = ERequestResponseDataMIME.ERDM_UNKNOWN

        self.mRequestDataBody = dataRemained[posEnd + 1 : ]
        self._pushLog("Requesting data parsing finished!")
        return 1

    def onResponseData(self) -> None:
        self.mStatus = EModelContextStatus.EMCS_RESPONSE_BEGIN
        if self.mResponseDataFlag == ERequestResponseDataFlag.ERDF_OK:
            mime = ''
            if self.mResponseDataMIME == ERequestResponseDataMIME.ERDM_XML_STREAM:
                mime = '[XML|STREAM]'
            elif self.mResponseDataMIME == ERequestResponseDataMIME.ERDM_ZIP_STREAM:
                mime = '[ZIP|STREAM]'
            elif self.mResponseDataMIME == ERequestResponseDataMIME.ERDM_RAW_STREAM:
                mime = '[RAW|STREAM]'
            elif self.mResponseDataMIME == ERequestResponseDataMIME.ERDM_XML_FILE:
                mime = '[XML|FILE]'
            elif self.mResponseDataMIME == ERequestResponseDataMIME.ERDM_ZIP_FILE:
                mime = '[ZIP|FILE]'
            elif self.mResponseDataMIME == ERequestResponseDataMIME.ERDM_RAW_FILE:
                mime = '[RAW|FILE]'
            else:
                mime = '[UNKNOWN]'
            self._pushLog("Response data start!")
            self._sendMessage('{onResponseData}' + self.mInstanceID + '&' + self.mCurrentState + '&' + self.mCurrentEvent + '&' + str(len(self.mResponseDataBody)) + '[OK]' + mime + self.mResponseDataBody )
            self.mStatus = EModelContextStatus.EMCS_RESPONSE

            self._wait4Status(EModelContextStatus.EMCS_RESPONSE_END)
        elif self.mResponseDataFlag == ERequestResponseDataFlag.ERDF_ERROR:
            self._sendMessage('{onResponseData}' + self.mInstanceID + '&' + self.mCurrentState + '&' + self.mCurrentEvent + '&0[ERROR]' )
            self.mStatus = EModelContextStatus.EMCS_RESPONSE
            self._wait4Status(EModelContextStatus.EMCS_RESPONSE_END)
        elif self.mResponseDataFlag == ERequestResponseDataFlag.ERDF_NOTREADY:
            self._sendMessage('{onResponseData}' + self.mInstanceID + '&' + self.mCurrentState + '&' + self.mCurrentEvent + '&' + len(self.mResponseDataBody))
            self.mStatus = EModelContextStatus.EMCS_RESPONSE
            self._wait4Status(EModelContextStatus.EMCS_RESPONSE_END)
            self._pushLog("Response data finished!")
        
        self._resetResponseDataInfo()

    def onPostErrorInfo(self, errinfo: str) -> None:
        self.mStatus = EModelContextStatus.EMCS_POST_BEGIN
        self._pushLog("Posting error message: [" + errinfo + "]")
        self._sendMessage('{onPostErrorInfo}' + self.mInstanceID + '&' + errinfo)
        self.mStatus = EModelContextStatus.EMCS_POST
        self._wait4Status(EModelContextStatus.EMCS_POST_END)

    def onPostWarningInfo(self, warninginfo: str) -> None:
        self.mStatus = EModelContextStatus.EMCS_POST_BEGIN
        self._pushLog("Posting warning message: [" + warninginfo + "]")
        self._sendMessage('{onPostMessageInfo}' + self.mInstanceID + '&' + warninginfo)
        self.mStatus = EModelContextStatus.EMCS_POST
        self._wait4Status(EModelContextStatus.EMCS_POST_END)

    def onPostMessageInfo(self, messageinfo: str) -> None:
        self.mStatus = EModelContextStatus.EMCS_POST_BEGIN
        self._pushLog("Posting message: [" + messageinfo + "]")
        self._sendMessage('{onPostMessageInfo}' + self.mInstanceID + '&' + messageinfo)
        self.mStatus = EModelContextStatus.EMCS_POST
        self._wait4Status(EModelContextStatus.EMCS_POST_END)

    def onLeaveState(self) -> None:
        self.mStatus = EModelContextStatus.EMCS_STATE_LEAVE_BEGIN
        self._sendMessage('{onLeaveState}' + self.mInstanceID + '&' + self.mCurrentState)
        self.mStatus = EModelContextStatus.EMCS_STATE_LEAVE
        self._pushLog("State leaved!")
        self._wait4Status(EModelContextStatus.EMCS_STATE_LEAVE_END)
    
    def onFinalize(self) -> None:
        self.mStatus = EModelContextStatus.EMCS_FINALIZE_BEGIN
        self._sendMessage('{onFinalize}' + self.mInstanceID)
        self.mStatus = EModelContextStatus.EMCS_FINALIZE
        self._wait4Status(EModelContextStatus.EMCS_FINALIZE_END)
        self._pushLog("Model finalized!")
        # self.mMornitoringThread.join()
        self._pushLog("Model thread joined!")
        sys.exit()

    def onGetModelAssembly(self, methodName: str) -> str:
        self.mStatus = EModelContextStatus.EMCS_COMMON_BEGIN
        self._sendMessage('{onGetModelAssembly}' + self.mInstanceID + '&' + methodName)
        self.mStatus = EModelContextStatus.EMCS_COMMON_REQUEST
        self._wait4Status(EModelContextStatus.EMCS_COMMON_END)
        assembly = self.mData[0 : self.mData.index('}') + 1]
        return assembly

    def initControlParam(self) -> None:
        self.mStatus = EModelContextStatus.EMCS_INIT_CTRLPARAM_BEGIN
        self._sendMessage('{onInitControlParam}' + self.mInstanceID)
        self.mStatus = EModelContextStatus.EMCS_INIT_CTRLPARAM
        self._wait4Status(EModelContextStatus.EMCS_INIT_CTRLPARAM_END)
        
        posEnd = self.mData.index('&')
        controlParamBuffer = self.mData[posEnd + 1 : ]

        try:
            self.mControlParams = json.loads(controlParamBuffer)
        except ZeroDivisionError as ex:
            pass

    #####! Data exchanging functions
    def getRequestDataFlag(self) -> ERequestResponseDataFlag:
        return self.mRequestDataFlag

    def getRequestDataMIME(self) -> ERequestResponseDataMIME:
        return self.mRequestDataMIME

    def getRequestDataBody(self) -> str:
        return self.mRequestDataBody

    def setResponseDataFlag(self, flag: ERequestResponseDataFlag) -> None:
        self.mResponseDataFlag = flag

    def setResponseDataMIME(self, MIME: ERequestResponseDataMIME) -> None:
        self.mResponseDataMIME = MIME

    def setResponseDataBody(self, body: str) -> None:
        self.mResponseDataBody = body

    def getResponseDataFlag(self) -> ERequestResponseDataFlag:
        return self.mResponseDataFlag

    def getResponseDataMIME(self) -> ERequestResponseDataMIME:
        return self.mResponseDataMIME

    def getResponseDataDody(self) -> str:
        return self.mResponseDataBody

    def getCurrentStatus(self) -> EModelContextStatus:
        return self.mStatus

    def getProcessParam(self, key: str) -> str:
        return self.mProcessParams.get(key, None)

    def setProcessParam(self, key: str, value: str):
        self.mProcessParams[key] = value
        self._sendProcessParam()
        
    def getRequestDataByFile(self) -> str:
        pass

    def getRequestDataByStream(self) -> str:
        pass

    #####! Running status information functions
    def getCurrentRunningState(self) -> str:
        return self.mCurrentState
        
    def getCurrentRunningEvent(self) -> str:
        return self.mCurrentEvent

    #####! Directory related functions
    def getCurrentDataDirectory(self) -> str:
        instanceDir = self.getModelInstanceDirectory()
        if os.path.exists(instanceDir) == False:
            os.makedirs(instanceDir) 
        stateDir = instanceDir + self.getCurrentRunningState() + self._getSlash()
        if os.path.exists(stateDir) == False:
            os.makedirs(stateDir) 
        eventDir = stateDir + self.getCurrentRunningEvent() + self._getSlash()
        if os.path.exists(eventDir) == False:
            os.makedirs(eventDir)
        return eventDir

    def getMappingLibraryDirectory(self) -> str:
        if self.mMappingLibDir[:-1] != self._getSlash():
            self.mMappingLibDir = self.mMappingLibDir + self._getSlash()
        return self.mMappingLibDir

    def getModelInstanceDirectory(self) -> str:
        if self.mInstanceDir[:-1] != self._getSlash():
            self.mInstanceDir = self.mInstanceDir + self._getSlash()
        return self.mInstanceDir
    
    #! Get model directory
    def onGetModelDirContext(self) -> str:
        self.mStatus = EModelContextStatus.EMCS_COMMON_BEGIN
        self._sendMessage('{onGetModelContext}' + self.mInstanceID)
        self.mStatus = EModelContextStatus.EMCS_COMMON_REQUEST
        self._wait4Status(EModelContextStatus.EMCS_COMMON_END)
        modelDir = self.mData
        self.mModelDir = modelDir
        return self.mModelDir
    
    def getModelDirectory(self) -> str:
        return self.mModelDir
    
    def getDataFileByExt(self, ext: str) -> list:
        ext = ext.lower()
        dire = self.getCurrentDataDirectory()
        list_files = os.listdir(dire)
        list_f = []
        for filepath in list_files:
            if ext == ModelServiceContext.getFileExtension(filepath).lower():
                list_f.append(dire + filepath)
        return list_f

    def mapZipToCurrentDataDirectory(self, zipf: str) -> None:
        z = zipfile.ZipFile(zipf, 'r')
        dire = self.getCurrentDataDirectory()
        z.extractall(path = dire)
        z.close()

    def makeZipFile(self, dirpath: str, outFullName: str, skips: list = []) -> str:
        mark = True
        for path, dirnames, filenames in os.walk(dirpath):
            if mark:
                zip = zipfile.ZipFile(outFullName, "w", zipfile.ZIP_DEFLATED)
                mark = False
            fpath = path.replace(dirpath,'')
            for filename in filenames:
                ext = os.path.splitext(filename)[1]
                mark = False
                for skip in skips:
                    if skip == ext:
                        mark = True
                        break
                if mark:
                    continue
                zip.write(os.path.join(path,filename),os.path.join(fpath,filename))
        zip.close()

    #####! Log functions
    def exportLogFile(self, path: str) -> None:
        f = open(path, "w")
        f.write(self.exportLog())
        f.close()

    def exportLog(self) -> str:
        logs = ""
        for index in range(len(self.mLogs)):
            logs = logs + self.mLogs[index].export() + "\n"
        return logs

    #####! Common invoking functions
    def startProcess(self, path: str) -> None:
        pass

    def shell(self) -> None:
        pass

    @staticmethod
    def Monitoring_thread(ms):
        while True:
            data = ms._receiveMessage()
            strCmds = data.split('\n')
            for cmd in strCmds:
                if cmd[-1:] == "'":
                    cmd = cmd[:-1]
                header = cmd[cmd.index('{') : cmd.index('}') + 1]
                ms.mData = cmd[cmd.index('}') + 1 : ]
                if header == '{Initialized}':
                    ms.mStatus = EModelContextStatus.EMCS_INIT_END
                elif header == '{Enter State Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_STATE_ENTER_END
                elif header == '{Fire Event Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_EVENT_END
                elif header == '{Request Data Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_REQUEST_END
                elif header == '{Response Data Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_RESPONSE_END
                elif header == '{Response Data Received}':
                    ms.mStatus = EModelContextStatus.EMCS_RESPONSE_END
                elif header == '{Post Error Info Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_POST_END
                elif header == '{Post Warning Info Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_POST_END
                elif header == '{Post Message Info Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_POST_END
                elif header == '{Leave State Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_STATE_LEAVE_END
                elif header == '{Finalize Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_FINALIZE_END
                    return
                elif header == '{GetModelAssembly Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_COMMON_END
                elif header == '{SetControlParams Notified}':
                    #TODO Control Parameter
                    pass
                elif header == '{GetModelDirContext Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_COMMON_END
                elif header == '{GetModelRunningLog Notified}':
                    ms.mStatus = EModelContextStatus.EMCS_COMMON_END
                elif header == '{kill}':
                    return sys.exit()
                else :
                    print('Unknown Command!')
                    pass
    