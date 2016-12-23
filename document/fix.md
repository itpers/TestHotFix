### 原理：

* android打包流程

  将java文件编译成class文件---将class文件打包成classes.dex文件---将dex文件打包成apk--进行签名


* 来源于dex分包方案

  1. 分包原因：

     一个dvm中存储方法id用的是short类型，导致dex中方法不能超过65536个

     apk在android 2.3之前的机器无法安装，因为dex文件过大（用来执行dexopt的内存只分配了5M）

  2. 解决方法

     将编译好的class文件拆分打包成两个dex，绕过dex方法数量的限制以及安装时的检查，在运行时再动态加载第二个dex文件中。

     除了第一个dex文件（即正常apk包唯一包含的Dex文件），其它dex文件都以资源的方式放在安装包中，并在Application的onCreate回调中被注入到系统的ClassLoader。

* 热补丁修复技术

  1. 根据上述原理，把要修复的类单独打包成dex，在应用启动的时候注入到系统的ClassLoader中，根据类的查找原则覆盖原有的类，达到修复目的；

     ![](http://mmbiz.qpic.cn/mmbiz/0aYRVN1mAJwR6vqR4Yv6V3zIvjqmgdu7dVfrXN7XxhOjiaahHricl00hal4rjw1cQ2LRFKVGU7uUOO0Q5HSz7hKw/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1)

  2. 类加载过程

       ```
         ClassLoader
           -- BaseClassLoader 
               -- PathClassLoader Android系统所使用的加载系统类和已安装的apk
               -- DexClassLoader 可以加载SDK中未安装的jar/dex/apk
       ```
       BaseDexClassLoader --- (pathList)DexPathList -- (dexElement)DexElement

### 方法

  1. apk的classes.dex可以从应用本身的PathClassLoader中获取。
  2. 补丁包的dex需要new一个DexClassLoader加载后再获取。
  3. 分别通过反射取出dex文件，重新合并成一个数组，然后赋值给PathClassLoader的dexElements


### 问题

* CLASS_ISPREVERIFIED标记

  如果A类的 static， private，构造函数， override方法 中直接引用到的类和A类都在同一个dex中，那么这个类就会被打上CLASS_ISPREVERIFIED标记，被打上这个标记的类不能引用其他dex中的类

* 代码注入操作

* 混淆问题

* 自动打补丁包

### 解决方案

* 针对 CLASS_ISPREVERIFIED标记

  在所有类的构造函数中引用另一个dex中的类

  因为要引入另一个dex中的类，无法在源码中直接插入，所以使用javassist直接对.class文件进行代码注入操作

* 代码注入操作

  由于项目使用Gradle构建，编译打包签名等均自动化完成，所以需要hook gradle相关task，在类被编译成.class文件后被打包成dex文件前，使用javassist来对.class文件进行代码注入

* 混淆问题

  开启混淆后，类名被替换，补丁包无法被识别

* 自动打补丁包

  1. 发正式包时生成所有类的md5值并保存
  2. 打补丁包时对比md5值，针对修改过的类生成补丁包
  3. 加载补丁包时进行签名验证

### 注意
在Mac上需要将 **buildsrc**下 **FixUtils.java**  中  **static void dx()** 方法中执行**dex**打包的命令中的**dx.bat**改为**dx**

### 相关网站

* [Android热补丁方案](http://mp.weixin.qq.com/s?__biz=MzI1MTA1MzM2Nw==&mid=400118620&idx=1&sn=b4fdd5055731290eef12ad0d17f39d4a&scene=1&srcid=1031x2ljgSF4xJGlH1xMCJxO&uin=MjAyNzY1NTU=&key=04dce534b3b035ef58d8714d714d36bcc6cc7e136bbd64850522b491d143aafceb62c46421c5965e18876433791d16ec&devicetype=iMac%20MacBookPro12,1%20OSX%20OSX%2010.10.5%20build%2814F27%29&version=11020201&lang=zh_CN&pass_ticket=7O/VfztuLjqu23ED2WEkvy1SJstQD4eLRqX%2b%2bbCY3uE=)
* [Gradle脚本基础](http://blog.csdn.net/yanbober/article/details/49314255)
* [Gradle插件开发](http://blog.csdn.net/sbsujjbcy/article/details/50782830)





