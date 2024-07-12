# ContainerEngine

## 介绍
该项目（容器交互引擎）充当了模型封装代码与Docker之间的中间件，它接收交互接口的各种请求并进行相应处理，然后通过调用Docker的API实现与Docker的交互。这种架构使得封装代码与Docker之间的通信变得更加灵活和高效。用户只需关注交互接口的使用，而无需深入了解底层的Docker实现细节，他们只需知道Docker提供了一个独立的环境供模型使用即可。

<img src="https://blog-images-1301988137.cos.ap-nanjing.myqcloud.com/blog%2F2407%2F%E5%AE%B9%E5%99%A8%E4%BA%A4%E4%BA%92%E5%BC%95%E6%93%8E%E4%BD%9C%E7%94%A8%E7%A4%BA%E6%84%8F%E5%9B%BE.png"  />
