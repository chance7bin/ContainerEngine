from modelservicecontext import EModelContextStatus
from modelservicecontext import ERequestResponseDataFlag
from modelservicecontext import ERequestResponseDataMIME
from modelservicecontext import ModelServiceContext
from modeldatahandler import ModelDataHandler
import sys
import os
from taskcontroller import TaskController

# encapsulation program
# Begin encapsulation
if len(sys.argv) < 3:
	exit()

ms = ModelServiceContext()
ms.onInitialize(sys.argv[1], sys.argv[2], sys.argv[3])
mdh = ModelDataHandler(ms)

# try-catch写法
try:

    # 初始化TaskController
    tc = TaskController()
    config_path = os.path.join(ms.getModelDirectory(), "env_config.xml")
    tc.init(config_path, ms)

    # Enter State
    ms.onEnterState('run')

    # Event
    ms.onFireEvent('inputLanguageFile')

    ms.onRequestData()

    if ms.getRequestDataFlag() == ERequestResponseDataFlag.ERDF_OK:
        if ms.getRequestDataMIME() == ERequestResponseDataMIME.ERDM_RAW_FILE:
            lang_file_path = ms.getRequestDataBody()
    else:
        ms.onFinalize()

    # Event
    ms.onFireEvent('inputTextFile')

    ms.onRequestData()

    if ms.getRequestDataFlag() == ERequestResponseDataFlag.ERDF_OK:
        if ms.getRequestDataMIME() == ERequestResponseDataMIME.ERDM_RAW_FILE:
            file_path = ms.getRequestDataBody()
            
    else:
        ms.onFinalize()

    # Event
    ms.onFireEvent('outputResult')

    f = open(lang_file_path, "r")
    lang = f.read().replace('\n', "")
    f.close()

    instance_dir = os.path.normpath(ms.getModelInstanceDirectory())
    if not os.path.exists(instance_dir):
        os.makedirs(instance_dir)
    result_path = os.path.normpath(instance_dir + "/result.png")
    # print(result_path)

    # createWC(lang, file_path, result_path)

    # 执行上传文件并返回上传路径
    # 根据filepath获取文件名
    file_name = os.path.basename(file_path)
    remote_input_path = '/home/model/tmp/' + file_name
    tc.upload(file_path, remote_input_path)

    remote_result_path = "/home/model/tmp/result.png"
    
    tc.invoke("runmodel", lang, remote_input_path, remote_result_path)
    
    tc.download(remote_result_path, result_path)

    ms.setResponseDataFlag(ERequestResponseDataFlag.ERDF_OK)
    ms.setResponseDataMIME(ERequestResponseDataMIME.ERDM_RAW_FILE)
    ms.setResponseDataBody(result_path)

    ms.onResponseData()
    # Leave State
    ms.onLeaveState()

    tc.finalize()

    ms.onFinalize()

except Exception as e:
    state = ms.getCurrentRunningState()
    event = ms.getCurrentRunningEvent()
    ms.onPostErrorInfo(f"[{state}] --- [{event}] --- Exception: {e}")
    tc.finalize()
