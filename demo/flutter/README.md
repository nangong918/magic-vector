**flutterDemo**
====





### 关于配置智能语音助手

需要将key配置在：[module_key.json](flutternew/assets/module_key.json)

* LLM大模型：[讯飞模型](https://training.xfyun.cn/modelService)
* 离线语音唤醒：[讯飞离线语音唤醒AndroidSDK](https://console.xfyun.cn/services/aikit-awaken)
* STT语音转文字：[讯飞中文STT](https://console.xfyun.cn/services/bmc)


注册并申请这些功能然后把对应的key配置在`module_key.json`

### 项目入口

flutternew是flutter的demo
flutteraar是flutter中Android原生接口的aar包，更新功能之后需要去flutteraar的aarlib的gradle中进行assemble
然后再在output中取出打出的release aar包，并拷贝到flutternew的`android/app/libs`中



### 关于离线唤醒SDK特别注意

离线唤醒SDK比较特别，不仅仅是继承aar就可以了。
[详情参考](https://www.xfyun.cn/doc/asr/AIkit_awaken/Android-SDK.html)
需要把它的SDK的一部分东西拷贝到设备的SD卡中，详细信息上述文档写的有。
当然后续开发者也可以优化，把SDK的资源放在assets下，在代码中直接拷贝到目标设备中。










