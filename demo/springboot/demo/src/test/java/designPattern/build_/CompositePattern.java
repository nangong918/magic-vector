package designPattern.build_;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/11/6 13:38
 * 组合模式
 * 多个类聚合在一起组成对象而不是继承，基本的网络请求的JSON传递就是组合模式
 */
public class CompositePattern {

    // 组件接口
    interface Employee {
        void showDetails();
    }

    // 叶子节点类：员工
    static class Developer implements Employee {
        private final String name;

        public Developer(String name) {
            this.name = name;
        }

        @Override
        public void showDetails() {
            System.out.println("Developer: " + name);
        }
    }

    // 叶子节点类：经理
    static class Manager implements Employee {
        private final String name;

        public Manager(String name) {
            this.name = name;
        }

        @Override
        public void showDetails() {
            System.out.println("Manager: " + name);
        }
    }

    // 组合类：部门
    static class Department implements Employee {
        private final String name;
        private final List<Employee> employees = new ArrayList<>();

        public Department(String name) {
            this.name = name;
        }

        public void addEmployee(Employee employee) {
            employees.add(employee);
        }

        public void removeEmployee(Employee employee) {
            employees.remove(employee);
        }

        @Override
        public void showDetails() {
            System.out.println("Department: " + name);
            for (Employee employee : employees) {
                employee.showDetails();
            }
        }
    }

    // 测试类
    public static class CompositePatternDemo {
        public static void main(String[] args) {
            // 创建员工
            Developer dev1 = new Developer("Alice");
            Developer dev2 = new Developer("Bob");
            Manager manager1 = new Manager("Charlie");

            // 创建部门并添加员工
            Department engineeringDepartment = new Department("Engineering");
            engineeringDepartment.addEmployee(dev1);
            engineeringDepartment.addEmployee(dev2);
            engineeringDepartment.addEmployee(manager1);

            // 创建另一个部门
            Department hrDepartment = new Department("HR");
            hrDepartment.addEmployee(new Manager("Diana"));

            // 创建根部门并添加部门
            Department organization = new Department("Organization");
            organization.addEmployee(engineeringDepartment);
            organization.addEmployee(hrDepartment);

            // 显示组织结构
            organization.showDetails();
        }
    }
}
