package designPattern.create;


import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author 13225
 * @date 2025/11/6 11:59
 * 防止对象的指针引用，而是使用复制
 */
public class PrototypePattern {

    // PrototypeDemo.java

    @Setter
    @Getter
    static class Prototype implements Cloneable, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        private String string;

        private SerializableObject obj;

        /* 浅复制 */
        public Object clone() throws CloneNotSupportedException {
            Prototype proto = (Prototype) super.clone();
            return proto;
        }

        /* 深复制 */
        public Object deepClone() throws IOException, ClassNotFoundException {

            /* 写入当前对象的二进制流 */
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);

            /* 读出二进制流产生的新对象 */
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return ois.readObject();
        }

    }

    static class SerializableObject implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    // 测试类
    public static class PrototypeDemo {
        public static void main(String[] args) {
            Prototype proto = new Prototype();
            proto.setString("testString");
            proto.setObj(new SerializableObject());
            try {
                Prototype cloneProto = (Prototype) proto.clone();
                System.out.println(cloneProto.getString());
                Prototype deepCloneProto = (Prototype) proto.deepClone();
                System.out.println(deepCloneProto.getString());
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

}
