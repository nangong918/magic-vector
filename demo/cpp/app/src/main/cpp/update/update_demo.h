#ifndef CPPDEMO_UPDATE_DEMO_H
#define CPPDEMO_UPDATE_DEMO_H

// 仅保留必要头文件（JNI+线程+原子变量）
#include <jni.h>
#include <atomic>
#include <thread>
#include <chrono>
#include <syslog.h>

// 核心类：自定义命名update_demo
class update_demo {
public:
    // 单例（全局唯一实例，方便调用）
    static update_demo& get_instance();

private:
    // 禁止拷贝/赋值（单例必备）
    update_demo() = default;
    ~update_demo();
    update_demo(const update_demo&) = delete;
    update_demo& operator=(const update_demo&) = delete;

public:
    /**
     * 初始化JNI环境（保存跨线程调用的核心引用）
     * @param jvm: JVM全局引用
     * @param callback_obj: Java回调对象（全局引用）
     * @param msg_class: Java消息类（IntMsg）的Class引用
     */
    void init_jni_env(JavaVM* jvm, jobject callback_obj, jclass msg_class);

    /**
     * 启动线程池推送int++消息
     * @param interval_ms: 推送间隔（毫秒，默认1000ms）
     */
    void start_push_int_msg(int interval_ms = 1000);

    /**
     * 停止推送消息，释放资源
     */
    void stop_push_int_msg();

private:
    /**
     * 线程任务：循环生成int++消息并回调Java
     * @param interval_ms: 推送间隔
     */
    void push_int_msg_task(int interval_ms);

    /**
     * 创建Java层的IntMsg对象（仅包含int字段）
     * @param env: JNIEnv指针
     * @param int_value: 要推送的int值
     * @return: Java对象引用
     */
    jobject create_int_msg_obj(JNIEnv* env, int int_value);

private:
    // JNI核心全局引用（跨线程必备）
    JavaVM* m_jvm = nullptr;          // JVM全局引用
    jobject m_java_callback_obj = nullptr; // Java回调对象（全局引用）
    jclass m_java_msg_class = nullptr;     // Java消息类（IntMsg）全局引用

    // 线程控制
    std::atomic<bool> m_is_running{false}; // 线程运行标志
    std::thread m_push_thread;             // 推送线程
    std::atomic<int> m_int_value{0};       // 要推送的自增int值
};

#endif //CPPDEMO_UPDATE_DEMO_H