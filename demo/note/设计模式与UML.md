**设计模式与UML**
====



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
[FactoryMethod.java](../../springboot/demo/src/test/java/designPattern/FactoryMethod.java)
工厂模式：设计对象的属性，然后根据创建不同的对象实现设计的属性。

#### 抽象工厂模式
[AbstractFactoryPattern.java](../../springboot/demo/src/test/java/designPattern/AbstractFactoryPattern.java)
工厂方法模式有一个问题就是，类的创建依赖工厂类，也就是说，如果想要拓展程序，必须对工厂类进行修改，这违背了开闭原则。
Spring种的Service和ServiceImpl的关系就是抽象工厂模式
例子：PayService需要对应不同的国家：JP，CN，US。然后不同韩国的实现：JPPayImpl，CNPayImpl，USPayImpl；这就是一个抽象工厂模式

#### 单例模式
[SingletonPattern.java](../../springboot/demo/src/test/java/designPattern/SingletonPattern.java)
单例模式：保证一个类只有一个实例，并提供一个全局访问点。对资源统一管理。
Spring框架的@Component和@Service都是单例模式。注意有些时候需要避免单例模式，比如Websocket的每个链接都是独立的而不是单例。所以数据不能作为单例管理。

#### 构建模式
[BuilderPattern.java](../../springboot/demo/src/test/java/designPattern/BuilderPattern.java)
建造者模式：将一个复杂对象的构建与它的表示分离，使得同样的构建过程可以创建不同的表示。
lombok可以直接实现，相当于是数据填充。

#### 原型模式
[PrototypePattern.java](../../springboot/demo/src/test/java/designPattern/PrototypePattern.java)
目的是防止对象的指针引用，而是使用复制对象的数据

# 框架设计模式
## SpringBoot设计模式



## Android设计模式

Mvc，Mvp，Mvvm


# UML


