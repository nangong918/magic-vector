package designPattern.action;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/11/6 14:09
 * 迭代器模式：逐个循环
 */
public class IteratorPattern {

    // 迭代器接口
    interface Iterator<T> {
        boolean hasNext();
        T next();
    }

    // 聚合接口
    interface BookCollection {
        Iterator<Book> createIterator();
    }

    // 具体聚合类：书籍集合
    static class MyBookCollection implements BookCollection {
        private final List<Book> books = new ArrayList<>();

        public void addBook(Book book) {
            books.add(book);
        }

        @Override
        public Iterator<Book> createIterator() {
            return new BookIterator(books);
        }
    }

    // 具体迭代器类
    static class BookIterator implements Iterator<Book> {
        private final List<Book> books;
        private int position = 0;

        public BookIterator(List<Book> books) {
            this.books = books;
        }

        @Override
        public boolean hasNext() {
            return position < books.size();
        }

        @Override
        public Book next() {
            return books.get(position++);
        }
    }

    // 书籍类
    @Getter
    static class Book {
        private final String title;

        public Book(String title) {
            this.title = title;
        }

    }

    // 测试类
    public static class IteratorPatternDemo {
        public static void main(String[] args) {
            MyBookCollection bookCollection = new MyBookCollection();
            bookCollection.addBook(new Book("The Catcher in the Rye"));
            bookCollection.addBook(new Book("To Kill a Mockingbird"));
            bookCollection.addBook(new Book("1984"));

            // 创建迭代器
            Iterator<Book> iterator = bookCollection.createIterator();

            // 遍历书籍
            while (iterator.hasNext()) {
                Book book = iterator.next();
                System.out.println("Book title: " + book.getTitle());
            }
        }
    }

}
