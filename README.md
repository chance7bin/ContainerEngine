# ContainerEngine

## 介绍
该项目（容器交互引擎）充当了模型封装代码与Docker之间的中间件，它接收交互接口的各种请求并进行相应处理，然后通过调用Docker的API实现与Docker的交互。这种架构使得封装代码与Docker之间的通信变得更加灵活和高效。用户只需关注交互接口的使用，而无需深入了解底层的Docker实现细节，他们只需知道Docker提供了一个独立的环境供模型使用即可。


## 模型与容器交互方法设计

### 脚本调用结构化描述
<img src="./doc/images/1.png" width="80%" />


### 模型容器化交互标准设计
<img src="./doc/images/2.png" width="80%" />
<img src="./doc/images/3.png" width="80%" />

## 容器交互引擎设计
<img src="./doc/images/4.png" width="50%" />
<img src="./doc/images/5.png" width="80%" />
<img src="./doc/images/6.png" width="80%" />



