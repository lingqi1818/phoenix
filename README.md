什么是Phoenix？

*	用于解决php访问dubbo的问题。

*	php多进程之间无法共享长连接，除非用c实现hessian客户端并且所有结构内存分配都走共享内存，代价较高，所以我们开发了phoenix-proxy在本地开连接池服务，透明代理hessian来解决该问题。

*   phoenix-proxy还维护了服务发现和负载平衡的工作。


使用方法

*   cd ./phoenix-proxy
*   man clean install -Dmaven.test.skip
*   ./phoenix-server/bin/start.sh


配置说明

* 配置文件在/phoenix-server/conf/phoenix.properties中


* 配置项

    * phoenix.bufferSize=4096000000 ##本地读写缓冲区大小
	* phoenix.ip=127.0.0.1 ##服务绑定ip
	* phoenix.port=9898 ##服务绑定的端口
	* phoenix.serviers=com.fangcloud.phoenix.test.HelloService ##需要监听的dubbo服务，以,号或者;号分割
	* phoenix.registerAddr=127.0.0.1:2181 ##注册中心地址

