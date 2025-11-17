**设计模式、UML、面向对象**
====

# 面向对象

## 基本特征
封装，继承，多态

### 封装
封装(encapsulation)即信息隐蔽。它是指在确定系统的某一部分内容时，应考虑到其它部分的信息及联系都在这一部分的内部进行，外部各部分之间的信息联系应尽可能的少。
目的：隐藏对象的内部实现，只提供对象对外的功能。（解耦的一种形式）
封装的方法：private, protected, public

### 继承
继承：让某个类型的对象获得另一个类型的对象的属性和方法。继承就是子类继承父类的特征和行为，使得子类对象（实例）具有父类的实例域和方法，或子类从父类继承方法，使得子类具有父类相同的行为。

### 多态
多态：对于同一个行为，不同的子类对象具有不同的表现形式。

# Java23种设计模式

## 设计模式的分类

总体来说设计模式分为三大类：

创建型模式，共五种：工厂方法模式、抽象工厂模式、单例模式、建造者模式、原型模式。

结构型模式，共七种：适配器模式、装饰器模式、代理模式、外观模式、桥接模式、组合模式、享元模式。

行为型模式，共十一种：策略模式、模板方法模式、观察者模式、迭代子模式、责任链模式、命令模式、备忘录模式、状态模式、访问者模式、中介者模式、解释器模式。

其实还有两类：并发型模式、线程池模式

## 设计模式的六大原则

1、开闭原则（Open Close Principle）

开闭原则就是说对扩展开放，对修改关闭。在程序需要进行拓展的时候，不能去修改原有的代码，实现一个热插拔的效果。所以一句话概括就是：为了使程序的扩展性好，易于维护和升级。想要达到这样的效果，我们需要使用接口和抽象类，后面的具体设计中我们会提到这点。

2、里氏代换原则（Liskov Substitution Principle）

里氏代换原则(Liskov Substitution Principle LSP)面向对象设计的基本原则之一。 里氏代换原则中说，任何基类可以出现的地方，子类一定可以出现。 LSP是继承复用的基石，只有当衍生类可以替换掉基类，软件单位的功能不受到影响时，基类才能真正被复用，而衍生类也能够在基类的基础上增加新的行为。里氏代换原则是对“开-闭”原则的补充。实现“开-闭”原则的关键步骤就是抽象化。而基类与子类的继承关系就是抽象化的具体实现，所以里氏代换原则是对实现抽象化的具体步骤的规范。—— From Baidu 百科

3、依赖倒转原则（Dependence Inversion Principle）

这个是开闭原则的基础，具体内容：针对接口编程，依赖于抽象而不依赖于具体。

4、接口隔离原则（Interface Segregation Principle）

这个原则的意思是：使用多个隔离的接口，比使用单个接口要好。还是一个降低类之间的耦合度的意思，从这儿我们看出，其实设计模式就是一个软件的设计思想，从大型软件架构出发，为了升级和维护方便。所以上文中多次出现：降低依赖，降低耦合。

5、迪米特法则（最少知道原则）（Demeter Principle）

为什么叫最少知道原则，就是说：一个实体应当尽量少的与其他实体之间发生相互作用，使得系统功能模块相对独立。

6、合成复用原则（Composite Reuse Principle）

原则是尽量使用合成/聚合的方式，而不是使用继承。

### 创建型模式
#### 工厂模式
[FactoryMethod.java](../../springboot/demo/src/test/java/designPattern/create/FactoryMethod.java)
工厂模式：设计对象的属性，然后根据创建不同的对象实现设计的属性。

#### 抽象工厂模式
[AbstractFactoryPattern.java](../../springboot/demo/src/test/java/designPattern/create/AbstractFactoryPattern.java)
工厂方法模式有一个问题就是，类的创建依赖工厂类，也就是说，如果想要拓展程序，必须对工厂类进行修改，这违背了开闭原则。
Spring种的Service和ServiceImpl的关系就是抽象工厂模式
例子：PayService需要对应不同的国家：JP，CN，US。然后不同韩国的实现：JPPayImpl，CNPayImpl，USPayImpl；这就是一个抽象工厂模式

#### 单例模式
[SingletonPattern.java](../../springboot/demo/src/test/java/designPattern/create/SingletonPattern.java)
单例模式：保证一个类只有一个实例，并提供一个全局访问点。对资源统一管理。
Spring框架的@Component和@Service都是单例模式。注意有些时候需要避免单例模式，比如Websocket的每个链接都是独立的而不是单例。所以数据不能作为单例管理。

#### 构建模式
[BuilderPattern.java](../../springboot/demo/src/test/java/designPattern/create/BuilderPattern.java)
建造者模式：将一个复杂对象的构建与它的表示分离，使得同样的构建过程可以创建不同的表示。
lombok可以直接实现，相当于是数据填充。

#### 原型模式
[PrototypePattern.java](../../springboot/demo/src/test/java/designPattern/create/PrototypePattern.java)
目的是防止对象的指针引用，而是使用复制对象的数据；Spring框架的@Bean就是原型模式

### 结构型模式
#### 适配器模式
[AdapterPattern.java](../../springboot/demo/src/test/java/designPattern/build_/AdapterPattern.java)
适配器模式，根据适配者的特性展示不同的效果。
Android的RecyclerViewAdapter就会根据不同的View和ViewHolder进行不同类型的数据绑定并展现。

#### 装饰模式
[DecoratorPattern.java](../../springboot/demo/src/test/java/designPattern/build_/DecoratorPattern.java)
装饰器模式的本质确实是通过传入一个对象，并对其进行增强或修改，而不需要改变对象的结构。C/Cpp经常用到

#### 代理模式
[ProxyPattern.java](../../springboot/demo/src/test/java/designPattern/build_/ProxyPattern.java)
代理模式允许通过一个代理对象来控制对另一个对象的访问。这种模式常用于实现懒加载、访问控制、日志记录等功能

#### 外观模式（门面模式）
[FacadePattern.java](../../springboot/demo/src/test/java/designPattern/build_/FacadePattern.java)
外观模式的本质确实可以看作是一个“管理者”（或称为“门面”）类，它对多个子系统对象进行管理，并向外部提供一个统一的接口，从而简化了对这些子系统的访问

#### 桥接模式
[BridgePattern.java](../../springboot/demo/src/test/java/designPattern/build_/BridgePattern.java)
桥接模式
桥接模式就是将一个具体的对象拆分为多个接口
假设我们有一个绘图应用程序，可以绘制不同形状（如圆形和正方形），并通过不同的颜色（如红色和蓝色）进行填充。桥接模式可以帮助我们将形状接口和颜色接口的实现分离开来

#### 组合模式
[CompositePattern.java](../../springboot/demo/src/test/java/designPattern/build_/CompositePattern.java)
没什么讲的，多个类聚合在一起组成对象而不是继承，基本的网络请求的JSON传递就是组合模式

#### 享元模式
[FlyweightPattern.java](../../springboot/demo/src/test/java/designPattern/build_/FlyweightPattern.java)
享元模式：它通过共享对象来减少内存使用和提高性能。享元模式适合于需要大量相似对象的场景，它通过将对象的共享内存部分与可变部分分开，从而实现高效的资源管理。
不就是map找对象吗？Websocket的SessionManager就是这个模式

### 行为型模式
#### 策略模式
[StrategyPattern.java](../../springboot/demo/src/test/java/designPattern/action/StrategyPattern.java)
策略模式
封装算法

#### 模板方法模式
[TemplateMethodPattern.java](../../springboot/demo/src/test/java/designPattern/action/TemplateMethodPattern.java)
提前定义一个方法，按照模板方法进行执行
Android的Activity生命周期就是一个模板方法，允许开发者重写，但是不能改变生命周期调用顺序
必须使用abstract类 + final method，不能使用interface + default method，因为这样方法就可变，违背了模板方法不可变的规则。

#### 观察者模式
[ObserverPattern.java](../../springboot/demo/src/test/java/designPattern/action/ObserverPattern.java)
观察者模式，参考Android的liveData，还要考虑更新在哪个线程执行

#### 迭代器模式
[IteratorPattern.java](../../springboot/demo/src/test/java/designPattern/action/IteratorPattern.java)
迭代器模式：逐个循环

#### 责任链模式
[ChainOfResponsibilityPattern.java](../../springboot/demo/src/test/java/designPattern/action/ChainOfResponsibilityPattern.java)
责任链模式：找到一个能够处理的对象

#### 命令模式
[CommandPattern.java](../../springboot/demo/src/test/java/designPattern/action/CommandPattern.java)
命令模式
它将请求封装为一个对象，从而使你能够使用不同的请求、队列或日志请求，以及支持可撤销的操作
就是Restful的Request请求

#### 备忘录模式
[MementoPattern.java](../../springboot/demo/src/test/java/designPattern/action/MementoPattern.java)
备忘录模式: 创建一个可以撤销的缓存（创建一个Memory，让整个系统回归到Memory状态，比如尝试某个操作之前存储，如果发生异常就回到Memory，有些类似事务）

#### 状态模式
[StatePattern.java](../../springboot/demo/src/test/java/designPattern/action/StatePattern.java)
状态设计模式：内部的状态更变向外同步。Android的Activity生命周期就是如此，其他的生命周期管理也需要使用此设计模式

#### 访问者模式
[VisitorPattern.java](../../springboot/demo/src/test/java/designPattern/action/VisitorPattern.java)
它允许你在不改变被访问对象的情况下，对其结构进行操作。通过将操作封装到访问者类中，访问者模式可以使得添加新的操作变得更加灵活，而不需要修改被访问的对象结构

#### 中介模式
[MediatorPattern.java](../../springboot/demo/src/test/java/designPattern/action/MediatorPattern.java)
中介模式: 管理用户之间的通讯数据

#### 解释器模式
[InterpreterPattern.java](../../springboot/demo/src/test/java/designPattern/action/InterpreterPattern.java)

# 框架设计模式
## SpringBoot设计模式

AOP代理模式: 面向切面编程
IoC控制反转

## Android设计模式

Mvc，Mvp，Mvvm, Mvi, Jetpack Compose

### MVVM
viewModel可以在Activity中进行切换，实现activity的view根据数据切换
viewModel中的viewModelScope是伴随协程的生命周期，以viewModelScope启动的协程都会在viewModel结束时结束
livedata: 观察者模式，当数据变化时，会通知观察者
dataBinding: xml <-> viewModel自动绑定，在Jetpack Compose中取消了xml，view和viewModel的生命周期相同，该概念本质被实现而废弃。

Android设计准则：
UI：viewModel，Jetpack compose
自动注入：Darger
数据缓存：SharedPreferences，MKVV
数据持久化：Room
线程与生命周期：Kotlin协程，线程池
局部网络请求：ViewModelScope
全局网络请求：Service + EventBus （websocket长连接）
app内部的数据监听：EventBus
app系统级别的监听：BroadcastReceiver（如：网络状态监听，屏幕旋转监听等，飞行模式）-> BroadcastReceiver用来恢复websocket长连接
混淆与逆向：R8，JADX

# UML


