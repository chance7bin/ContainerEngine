#Data : 2023-12-5
#Author : qingbin chen

from subprocess import getoutput
import json
import platform
import time
from enum import Enum
import xml.etree.ElementTree as ET
import hashlib
import os
import zipfile
from modelservicecontext import ModelServiceContext

class TaskStatus(Enum):

    # 任务已经被创建，但是并未启动
    CREATED = "created"

    # 任务正在运行
    RUNNING = "running"

    # 任务已经完成
    FINISHED = "finished"

    # 任务已经失败
    FAILED = "failed"

    # 任务已经被删除
    DELETED = "deleted"

    # 其他状态
    OTHER = "other"

class ArgFormat(Enum):

    ARGUMENT = "argument"

    OPTION = "option"

    PATTERN = "pattern"


class TaskController:

    def __init__(self):
        self.engine_url = "http://localhost:8808"
        self.context = None
        self.container_id = "",
        self.tree = None
        self.tree_root = None
        self.adl = None # argrument declaration list

    def init(self, config_path, context : ModelServiceContext = None):

        self.context = context

        # 解析配置文件
        self.post_message("parse config file...")
        self.parse_config(config_path)

        # 获取镜像信息
        self.post_message("get image info...")
        img_name, img_file, command = self.get_image_info()

        # 导入镜像
        self.post_message("load image to container engine...")
        image_id = self.load_image(img_name, img_file, config_path)
        # print("image_id: " + image_id)
        
        # 创建容器
        self.post_message("create container...")
        self.container_id = self.create_container(image_id, command, True)

        self.post_message("init success...")


    # 解析配置文件
    def parse_config(self, config_path):
        self.tree = ET.parse(config_path)
        self.tree_root = self.tree.getroot()
        self.adl = self.tree_root.find('Container').find('ScriptInfo').find('ArgsType').findall('ArgDeclaration')

    # 调用方法
    def invoke(self, script_name, *args):
        
        command = self.get_script_command(script_name)

        cmd, cmd_dir = command
        args = list(args)
        args.extend([""] * (cmd.count("{}") - len(args))) # 参数少于{}的个数时，用空字符串填充
        fill_values = [arg for arg in args]
        filled_command = cmd.format(*fill_values)

        invoke_cmd = f'cd {cmd_dir} \n {filled_command}'
        # print(invoke_cmd)

        self.exec_response(self.container_id, invoke_cmd)

    # 解析脚本内容
    def get_script_command(self, script_name):
        script_content = self.get_script_content(script_name)
        if script_content == "":
            raise Exception("script [" + script_name + "] not found")
        command = script_content.attrib['command']
        parmas = script_content.findall('Parameter')

        parmas_format = self.get_real_format(parmas)

        # 根据参数列表构建填充字符串
        # fill_values = [param.attrib['ref'] for param in parmas]
        # filled_command = command.format(*fill_values)
        command_format = command.format(*parmas_format)
        command_dir = script_content.attrib['dir']
        return command_format, command_dir
    
    # 获取参数的具体格式
    def get_real_format(self, parmas):
        res = []
        for param in parmas:
            # 查找与parameter ref相同name的ArgDeclaration元素
            parameter_ref = param.attrib['ref']

            # 在 adl 中找到ArgDeclaration的name属性与parameter_ref相同的元素
            arg_declaration = None
            for ad in self.adl:
                if ad.attrib['name'] == parameter_ref:
                    arg_declaration = ad
                    break
            if arg_declaration is None:
                raise Exception("ArgDeclaration not found")


            if arg_declaration.attrib['format'] == ArgFormat.ARGUMENT.value:
                res.append("{}")
            else:
                res.append(arg_declaration.attrib['value'])

        return res


    # 获取脚本内容
    def get_script_content(self, script_name):
        script_content = ""
        scripts = self.tree_root.find('Container').find('ScriptInfo').find('Scripts').findall('Script')
        for script in scripts:
            if script.attrib['name'] == script_name:
                script_content = script
                break
        return script_content

    def post_message(self, msg):
        if self.context is not None:
            self.context.onPostMessageInfo(msg)

    def get_image_info(self):
        image = self.tree_root.find('Image')

        # 获取img名称
        img_name = None
        if image.find('ImageName') is not None:
            img_name = image.find('ImageName').text

        img_file = image.find('ImageTar').text
        # 获取img的真实路径
        if self.context is not None:
            model_path = self.context.getModelDirectory()
            img_file = os.path.normpath(os.path.join(model_path, img_file))

        # 计算md5
        # md5 = image.find('ImageMd5').text
        # if md5 == "" or md5 is None:
        #     self.post_message("md5 is empty, calculate md5...")
        #     md5 = self.calculate_md5(img_file)
        #     image.find('ImageMd5').text = md5
        #     self.tree.write(config_path, encoding='utf-8')
        
        # 获取镜像启动命令
        command = None
        if image.find('Command') is not None:
            command = image.find('Command').text

        # 返回 img_file 和 md5
        return img_name, img_file, command
            

    # 获取容器交互引擎的容器id
    def get_container_id(self):
        return self.container_id
    
    # 设置容器交互引擎的容器id
    def set_container_id(self, container_id):
        self.container_id = container_id

    # 获取容器中相应目录的路径
    def get_contaienr_dir(self, dir_name):
        
        directories = self.tree_root.find('Container').find('DirectoryInfo').findall('Directory')
    
        for directory in directories:
            name = directory.get('name')
            path = directory.get('path')
            
            if name == dir_name:
                return path
        
        return None

    # 拆分返回消息，获取需要的信息
    def splitmsg(self, msg):
        st = msg.split('\n')[-1]
        js = json.loads(st)
        if js['code'] != 200:
            raise Exception(js['msg'])
        return js
    
    def json2str(self, json_obj):
        params_str = json.dumps(json_obj, indent=0).replace("\n","") 
        # window的command.exe不支持单引号，所以要处理一下命令：先转义双引号，然后把单引号改为双引号
        # https://blog.csdn.net/iningwei/article/details/107065687
        if platform.system() == "Windows": 
            # 如果是windows系统，需要将双引号转义
            params_str = params_str.replace('"', '\\"')
        return params_str
    
    def image_exist_by_md5(self, md5):
        exist_curl = f'curl --location --request GET  "{self.engine_url}/image/exist?md5={md5}"'
        output = getoutput(exist_curl)
        res_json = self.splitmsg(output)
        return res_json['data']
    
    def image_exist_by_name(self, img_name):
        exist_curl = f'curl --location --request GET  "{self.engine_url}/image/exist?imageName={img_name}"'
        output = getoutput(exist_curl)
        res_json = self.splitmsg(output)
        return res_json['data']

    def load_image(self, img_name, image_path, config_path):
        
        # 1.判断镜像名为img_name的镜像是否已经存在
        # 当img_name非空时请求接口
        if img_name != "" and img_name is not None:
            exist_info = self.image_exist_by_name(img_name)
            if exist_info['exist']:
                return exist_info['imageId']

        # 2.计算md5
        image = self.tree_root.find('Image')
        md5 = image.find('ImageMd5').text
        if md5 == "" or md5 is None:
            self.post_message("md5 is empty, calculate md5...")
            md5 = self.calculate_md5(image_path)
            image.find('ImageMd5').text = md5
            self.tree.write(config_path, encoding='utf-8')

        # 3.判断相同md5镜像是否已经存在
        exist_info = self.image_exist_by_md5(md5)
        if exist_info['exist']:
            return exist_info['imageId']

        # 4.上传镜像
        self.post_message("upload image tar to container engine...")
        load_curl = f'curl --location --request POST  "{self.engine_url}/image/load" \
            --form "file=@{image_path}" \
            --form "md5={md5}"'
        output = getoutput(load_curl)
        res_json = self.splitmsg(output)
        return res_json['data']

    # 上传本地文件至容器交互引擎中
    def upload(self, local_file_path, remote_file_path, cover="cover", location="container"):

        # 如果上传的是文件夹，要先压缩
        needDelete = False
        uncompress = False
        if os.path.isdir(local_file_path):
            # 压缩文件夹
            self.zip_folder(local_file_path, local_file_path + ".zip")
            local_file_path = local_file_path + ".zip"
            remote_file_path = remote_file_path + ".zip"
            needDelete = True
            uncompress = True

        # local_file_dir = "../"
        # local_file_name = "hpcUtils.py"
        # local_file_path = "E:/desktop_dir/MyHotkeyScript.ahk"
        upload_curl = f'curl --location --request POST  "{self.engine_url}/file/upload" \
            --form "file=@{local_file_path}" \
            --form "path={remote_file_path}" \
            --form "cover={cover}" \
            --form "location={location}" \
            --form "uncompress={uncompress}" \
            --form "containerId={self.container_id}"'
        # upload_curl = "curl --insecure --location --request POST  http://localhost:8808/file/upload --form 'file=@'./hpcUtils.py''"
        # output = call(upload_curl.split())
        output = getoutput(upload_curl)
        res_json = self.splitmsg(output)
        # print(res_json)

        # 删除压缩文件
        if needDelete:
            os.remove(local_file_path)

        return res_json['data']

    # 从容器交互引擎下载文件到本地上
    def download(self, remote_file_path, local_file_path):
        # local_file_dir = "../"
        # local_file_name = "hpcUtils.py"
        # local_file_path = "E:/desktop_dir/MyHotkeyScript.ahk"
        # hpc_result_path = "/test/123.pdf"
        # result_path = "E:/container-engine/file/456.pdf"

        # download_curl = f'curl --location --request GET  "{self.engine_url}/file/download?path={remote_file_path}&location=container&containerId={self.container_id}" \
        #     --output {local_file_path}'
        
        download_curl = (
            f'curl --location --request GET '
            f'"{self.engine_url}/file/download'
            f'?path={remote_file_path}'
            f'&location=container'
            f'&containerId={self.container_id}"'
            f' --output {local_file_path}'
        )

        getoutput(download_curl)

    # 判断容器交互引擎中是否存在某个文件
    def file_exist(self, remote_file_path):
        # remote_file_path = "/test/1223.pdf"
        exist_curl = f'curl --location --request GET  "{self.engine_url}/file/exist?path={remote_file_path}"'
        output = getoutput(exist_curl)
        res_json = self.splitmsg(output)
        # print(res_json)
        return res_json['data']

    # 执行容器交互引擎中的命令
    def exec(self, ins_id, script_content):
        # 多行命令时需在每行末尾加入\n 换行转义符 （使用&&时curl无法识别，需\&转义，所以规定使用\n，后端将\n转换为&&)

        # script_content = "cd /home/container/tmp \n python test.py"
        # script_content = "cd /opt \n ls"
        params = {
            "insId": ins_id,
            "script": script_content
        }
        # json转字符串
        # params_str = json.dumps(params, indent=0).replace("\n","") 
        # # window的command.exe不支持单引号，所以要处理一下命令：先转义双引号，然后把单引号改为双引号
        # # https://blog.csdn.net/iningwei/article/details/107065687
        # if platform.system() == "Windows": 
        #     # 如果是windows系统，需要将双引号转义
        #     params_str = params_str.replace('"', '\\"')
        params_str = self.json2str(params)

        exec_curl = f'curl --location --request POST  "{self.engine_url}/instance/task/actions/exec" \
            --header "Content-Type: application/json" \
            --data-raw "{params_str}"'
        output = getoutput(exec_curl)
        result_json = self.splitmsg(output)
        # print(result_json)
        return result_json['data']

    # 获取容器交互引擎中某个任务的执行状态
    def get_exec_status(self, exec_id):
        # execId = "79e02166a3688ec8839d85ca4d630c1e52fd19071f58607b698e2b78e7c8cd9f"
        curl = f'curl --location --request GET  "{self.engine_url}/instance/task/actions/exec/status?execId={exec_id}"'
        output = getoutput(curl)
        res_json = self.splitmsg(output)
        # print(res_json)
        return res_json['data']
    
    # 获取容器交互引擎中某个任务的执行信息
    def get_exec_info(self, exec_id):
        curl = f'curl --location --request GET  "{self.engine_url}/instance/task/actions/exec/info?execId={exec_id}"'
        output = getoutput(curl)
        res_json = self.splitmsg(output)
        # print(res_json)
        return res_json['data']

    # 判断容器交互引擎中某个任务是否执行完成    
    def if_exec_finished(self, exec_id):
        curl = f'curl --location --request GET  "{self.engine_url}/instance/task/actions/exec/ifdone?execId={exec_id}"'
        output = getoutput(curl)
        res_json = self.splitmsg(output)
        # print(res_json)
        return res_json['data']
    
    # 等待容器交互引擎中某个任务执行完成
    def wait_exec_result(self, exec_id):
        while True:
            if self.if_exec_finished(exec_id):
                break
            time.sleep(1)

    # 执行容器交互引擎中的命令，并等待执行结果
    def exec_response(self, ins_id, script_content):
        exec_id = self.exec(ins_id, script_content)
        self.wait_exec_result(exec_id)
        info = self.get_exec_info(exec_id)
        if info['status'] == TaskStatus.FAILED.value:
            raise Exception(info['stderr'])
    
    def calculate_md5(self, file_path):
        with open(file_path, 'rb') as file:
            md5_hash = hashlib.md5()
            while True:
                data = file.read(4096)
                if not data:
                    break
                md5_hash.update(data)
        return md5_hash.hexdigest()
    

    # 创建容器
    def create_container(self, image_id, command, new_container=True):
        """
        创建容器
        :param image_id: 镜像id
        :param command: 镜像启动命令
        :param new_container: 是否创建新容器
        """

        params = {
            "imageId": image_id,
            "cmd": command,
            "newContainer": new_container
        }

        params_str = self.json2str(params)

        create_curl = f'curl --location --request POST  "{self.engine_url}/instance/task/actions/start" \
            --header "Content-Type: application/json" \
            --data-raw "{params_str}"'
        output = getoutput(create_curl)
        res_json = self.splitmsg(output)
        return res_json['data']
    
    # 删除容器
    def delete_container(self, container_id):
        """
        删除容器
        :param container_id: 容器id
        """
        delete_curl = f'curl --location --request POST  "{self.engine_url}/instance/task/actions/delete?insId={container_id}"'
        output = getoutput(delete_curl)
        res_json = self.splitmsg(output)
        return res_json['data']

    # 压缩文件夹
    def zip_folder(self, folder_path, output_path):
        """
        压缩文件夹 (不能压缩到待压缩文件夹内，否则会出错)
        :param folder_path: 文件夹路径 ('path/to/your/folder')
        :param output_path: 压缩文件输出路径 ('path/to/output.zip)
        """
        with zipfile.ZipFile(output_path, 'w') as zipf:
            for root, _, files in os.walk(folder_path):
                for file in files:
                    file_path = os.path.join(root, file)
                    zipf.write(file_path, os.path.relpath(file_path, folder_path))

    
    # 任务结束，释放资源
    def finalize(self):
        self.post_message("release resource...")
        self.delete_container(self.container_id)
