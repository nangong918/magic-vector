#include "stl_demo.h"
#include <string>
#include <vector>
#include <map>
#include <algorithm>
#include <memory>    // 智能指针
#include <numeric>   // 数值算法
#include <sstream>   // 字符串流
#include <random>    // 随机数
#include <iostream>

using namespace std;

// 辅助函数：将C++字符串转换为jstring
jstring stringToJString(JNIEnv* env, const string& str) {
    return env->NewStringUTF(str.c_str());
}

// 辅助函数：生成带格式的输出
string formatOutput(const string& title, const string& content) {
    stringstream ss;
    ss << "=== " << title << " ===\n";
    ss << content << "\n";
    ss << "=======================\n\n";
    return ss.str();
}

extern "C" {

// 1. String测试
JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testString(JNIEnv* env, jobject thiz) {
    stringstream ss;

    ss << "1. 字符串创建和基本操作:\n";
    string str1 = "Hello";
    string str2 = "STL";
    string str3 = str1 + " " + str2 + "!";

    ss << "   str1: \"" << str1 << "\"\n";
    ss << "   str2: \"" << str2 << "\"\n";
    ss << "   str3 (拼接): \"" << str3 << "\"\n";
    ss << "   长度: " << str3.length() << "\n";
    ss << "   容量: " << str3.capacity() << "\n";

    ss << "\n2. 字符串查找和修改:\n";
    size_t pos = str3.find("STL");
    if (pos != string::npos) {
        ss << "   找到'STL'在位置: " << pos << "\n";
        str3.replace(pos, 3, "Standard Template Library");
        ss << "   替换后: \"" << str3 << "\"\n";
    }

    ss << "\n3. 子字符串和比较:\n";
    string sub = str3.substr(0, 5);
    ss << "   前5个字符: \"" << sub << "\"\n";
    ss << "   比较'Hello' == 'Hello': " << (str1 == "Hello") << "\n";

    return stringToJString(env, formatOutput("String测试", ss.str()));
}

// 2. Vector测试
JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testVector(JNIEnv* env, jobject thiz) {
    stringstream ss;

    ss << "1. 创建和初始化vector:\n";
    vector<int> numbers = {10, 20, 30, 40, 50};
    ss << "   初始值: ";
    for (int num : numbers) {
        ss << num << " ";
    }
    ss << "\n";

    ss << "\n2. 添加和删除元素:\n";
    numbers.push_back(60);
    numbers.push_back(70);
    ss << "   添加60,70后: ";
    for (int num : numbers) {
        ss << num << " ";
    }
    ss << "\n";

    numbers.pop_back();
    ss << "   删除最后一个后: ";
    for (int num : numbers) {
        ss << num << " ";
    }
    ss << "\n";

    ss << "\n3. 访问元素和容量:\n";
    ss << "   第一个元素: " << numbers.front() << "\n";
    ss << "   最后一个元素: " << numbers.back() << "\n";
    ss << "   索引2的元素: " << numbers[2] << "\n";
    ss << "   at(3): " << numbers.at(3) << "\n";
    ss << "   大小: " << numbers.size() << "\n";
    ss << "   容量: " << numbers.capacity() << "\n";

    ss << "\n4. 使用迭代器:\n";
    ss << "   遍历: ";
    for (int & number : numbers) {
        ss << number << " ";
    }
    ss << "\n";

    ss << "   反向遍历: ";
    for (auto it = numbers.rbegin(); it != numbers.rend(); ++it) {
        ss << *it << " ";
    }
    ss << "\n";

    ss << "\n5. 插入和删除:\n";
    auto insertPos = numbers.begin() + 2;
    numbers.insert(insertPos, 99);
    ss << "   在位置2插入99后: ";
    for (int num : numbers) {
        ss << num << " ";
    }
    ss << "\n";

    numbers.erase(numbers.begin() + 3);
    ss << "   删除位置3后: ";
    for (int num : numbers) {
        ss << num << " ";
    }
    ss << "\n";

    return stringToJString(env, formatOutput("Vector测试", ss.str()));
}

// 3. Map测试
JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testMap(JNIEnv* env, jobject thiz) {
    stringstream ss;

    ss << "1. 创建和初始化map:\n";
    map<string, int> studentScores;
    studentScores["Alice"] = 95;
    studentScores["Bob"] = 87;
    studentScores["Charlie"] = 92;
    studentScores["David"] = 78;

    ss << "   学生成绩表:\n";
    for (const auto& pair : studentScores) {
        ss << "     " << pair.first << ": " << pair.second << "\n";
    }

    ss << "\n2. 查找和访问元素:\n";
    auto it = studentScores.find("Bob");
    if (it != studentScores.end()) {
        ss << "   找到Bob的成绩: " << it->second << "\n";
    }

    ss << "   Alice的成绩: " << studentScores["Alice"] << "\n";
    ss << "   map大小: " << studentScores.size() << "\n";

    ss << "\n3. 检查键是否存在:\n";
    if (studentScores.count("Eve") == 0) {
        ss << "   Eve不在成绩表中\n";
    }

    ss << "\n4. 更新和删除:\n";
    studentScores["Alice"] = 98;  // 更新
    ss << "   更新Alice成绩后: " << studentScores["Alice"] << "\n";

    studentScores.erase("David");
    ss << "   删除David后大小: " << studentScores.size() << "\n";

    ss << "\n5. 遍历map的多种方式:\n";
    ss << "   使用auto: ";
    for (const auto& [name, score] : studentScores) {
        ss << name << ":" << score << " ";
    }
    ss << "\n";

    return stringToJString(env, formatOutput("Map测试", ss.str()));
}

// 4. Algorithm测试
JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testAlgorithm(JNIEnv* env, jobject thiz) {
    stringstream ss;

    vector<int> numbers = {7, 3, 9, 1, 5, 8, 2, 6, 4};

    ss << "1. 原始数据:\n";
    ss << "   ";
    for (int num : numbers) {
        ss << num << " ";
    }
    ss << "\n";

    ss << "\n2. 排序:\n";
    sort(numbers.begin(), numbers.end());
    ss << "   升序排序: ";
    for (int num : numbers) {
        ss << num << " ";
    }
    ss << "\n";

    ss << "\n3. 查找:\n";
    auto findIt = find(numbers.begin(), numbers.end(), 5);
    if (findIt != numbers.end()) {
        ss << "   找到5在位置: " << distance(numbers.begin(), findIt) << "\n";
    }

    auto lower = lower_bound(numbers.begin(), numbers.end(), 4);
    ss << "   第一个>=4的位置: " << distance(numbers.begin(), lower) << "\n";

    ss << "\n4. 最大最小值:\n";
    auto maxIt = max_element(numbers.begin(), numbers.end());
    auto minIt = min_element(numbers.begin(), numbers.end());
    ss << "   最大值: " << *maxIt << ", 最小值: " << *minIt << "\n";

    ss << "\n5. 反转:\n";
    reverse(numbers.begin(), numbers.end());
    ss << "   反转后: ";
    for (int num : numbers) {
        ss << num << " ";
    }
    ss << "\n";

    ss << "\n6. 数值算法:\n";
    int sum = accumulate(numbers.begin(), numbers.end(), 0);
    ss << "   求和: " << sum << "\n";

    ss << "\n7. 条件删除:\n";
    numbers.erase(
            remove_if(numbers.begin(), numbers.end(),
                    [](int x) { return x % 2 == 0; }),  // 删除偶数
            numbers.end()
    );
    ss << "   删除偶数后: ";
    for (int num : numbers) {
        ss << num << " ";
    }
    ss << "\n";

    return stringToJString(env, formatOutput("Algorithm测试", ss.str()));
}

// 5. Smart Pointer测试
JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testSmartPointer(JNIEnv* env, jobject thiz) {
    stringstream ss;

    ss << "1. unique_ptr (独占所有权):\n";
    {
        unique_ptr<int> ptr1 = make_unique<int>(42);
        ss << "   创建unique_ptr指向: " << *ptr1 << "\n";

        // unique_ptr不能复制，只能移动
        unique_ptr<int> ptr2 = move(ptr1);
        if (!ptr1) {
            ss << "   ptr1现在为空 (所有权已转移)\n";
        }
        ss << "   ptr2指向: " << *ptr2 << "\n";
        // 离开作用域，内存自动释放
    }

    ss << "\n2. shared_ptr (共享所有权):\n";
    {
        shared_ptr<int> ptr3 = make_shared<int>(100);
        ss << "   创建shared_ptr, use_count: " << ptr3.use_count() << "\n";

        {
            shared_ptr<int> ptr4 = ptr3;  // 共享所有权
            shared_ptr<int> ptr5 = ptr3;
            ss << "   复制2次后, use_count: " << ptr3.use_count() << "\n";
            ss << "   所有指针指向同一个值: " << *ptr3 << "\n";
        }  // ptr4, ptr5离开作用域

        ss << "   离开作用域后, use_count: " << ptr3.use_count() << "\n";
    }

    ss << "\n3. weak_ptr (弱引用):\n";
    {
        shared_ptr<int> shared = make_shared<int>(200);
        weak_ptr<int> weak = shared;

        ss << "   创建weak_ptr, use_count: " << shared.use_count() << "\n";

        if (auto temp = weak.lock()) {  // 尝试获取shared_ptr
            ss << "   weak_ptr锁定成功, 值: " << *temp << "\n";
        }

        shared.reset();  // 释放shared_ptr

        if (weak.expired()) {
            ss << "   weak_ptr已过期 (对象已被销毁)\n";
        }
    }

    ss << "\n4. 自定义删除器:\n";
    {
        // 修复：用引用捕获ss
        unique_ptr<int, function<void(int*)>> ptr(
                new int[5]{1, 2, 3, 4, 5},
                [&ss](const int* p) {  // 加上[&ss]引用捕获
                    ss << "   自定义删除器调用，删除数组\n";
                    delete[] p;
                }
        );
        ss << "   创建带自定义删除器的unique_ptr\n";
    }

    return stringToJString(env, formatOutput("Smart Pointer测试", ss.str()));
}

} // extern "C"