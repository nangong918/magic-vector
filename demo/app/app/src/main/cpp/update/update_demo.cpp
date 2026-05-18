#include "update_demo.h"

// 单例实现
update_demo& update_demo::get_instance() {
    static update_demo instance;
    return instance;
}

// 析构函数：释放JNI全局引用+停止线程
update_demo::~update_demo() {
    stop_push_int_msg(); // 先停止推送线程

    if (m_jvm && m_java_callback_obj && m_java_msg_class) {
        JNIEnv* env = nullptr;
        bool need_detach = false;

        // 1. 获取/附加JNIEnv
        int env_stat = m_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
        if (JNI_EDETACHED == env_stat) {
            if (m_jvm->AttachCurrentThread(&env, nullptr) != 0) {
                syslog(LOG_ERR, "JNI: Attach thread failed!");
                return;
            }
            need_detach = true;
        }

        // 2. 释放全局引用（避免GC问题）
        env->DeleteGlobalRef(m_java_callback_obj);
        env->DeleteGlobalRef(m_java_msg_class);

        // 3. 分离线程
        if (need_detach) {
            m_jvm->DetachCurrentThread();
        }
    }
}

// 初始化JNI环境（保存全局引用）
void update_demo::init_jni_env(JavaVM* jvm, jobject callback_obj, jclass msg_class) {
    if (!m_jvm && !m_java_callback_obj && !m_java_msg_class) {
        m_jvm = jvm;
        // 获取当前线程的JNIEnv
        JNIEnv* env = nullptr;
        m_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
        // 转为全局引用（核心：避免Java GC回收）
        m_java_callback_obj = env->NewGlobalRef(callback_obj);
        m_java_msg_class = (jclass)env->NewGlobalRef(msg_class);
    }
}

// 启动推送线程
void update_demo::start_push_int_msg(int interval_ms) {
    if (m_is_running) return;
    m_is_running = true;
    // 启动线程执行推送任务
    m_push_thread = std::thread(&update_demo::push_int_msg_task, this, interval_ms);
}

// 停止推送线程
void update_demo::stop_push_int_msg() {
    if (!m_is_running) return;
    m_is_running = false;
    // 等待线程结束
    if (m_push_thread.joinable()) {
        m_push_thread.join();
    }
}

// 核心：线程任务-推送int++消息到Java
void update_demo::push_int_msg_task(int interval_ms) {
    // 校验JNI环境
    if (!m_jvm || !m_java_callback_obj || !m_java_msg_class) {
        syslog(LOG_ERR, "JNI: Env not initialized!");
        m_is_running = false;
        return;
    }

    JNIEnv* env = nullptr;
    bool need_detach = false;

    // 1. 附加当前线程到JVM（C++线程必须附加才能调用JNI）
    int env_stat = m_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (JNI_EDETACHED == env_stat) {
        if (m_jvm->AttachCurrentThread(&env, nullptr) != 0) {
            syslog(LOG_ERR, "JNI: Attach thread failed!");
            m_is_running = false;
            return;
        }
        need_detach = true;
    }

    // 2. 获取Java回调方法ID（方法名/签名与Java层严格匹配）
    jclass callback_cls = env->GetObjectClass(m_java_callback_obj);
    jmethodID callback_method = env->GetMethodID(
            callback_cls,
            "onIntMsgReceived",          // Java回调方法名
            "(Lcom/vectordemo/domain/entity/kni/IntMsg;)V" // 方法签名：参数为IntMsg对象，返回void
    );
    if (!callback_method) {
        syslog(LOG_ERR, "JNI: Get callback method failed!");
        if (need_detach) m_jvm->DetachCurrentThread();
        m_is_running = false;
        return;
    }

    // 3. 循环推送int++消息
    while (m_is_running) {
        int current_val = ++m_int_value; // int自增
        // 创建Java消息对象
        jobject msg_obj = create_int_msg_obj(env, current_val);
        if (!msg_obj) {
            syslog(LOG_ERR, "JNI: Create IntMsg failed!");
            std::this_thread::sleep_for(std::chrono::milliseconds(interval_ms));
            continue;
        }

        // 4. 回调Java方法（推送消息）
        env->CallVoidMethod(m_java_callback_obj, callback_method, msg_obj);

        // 5. 释放本地引用（避免内存泄漏）
        env->DeleteLocalRef(msg_obj);

        // 间隔等待
        std::this_thread::sleep_for(std::chrono::milliseconds(interval_ms));
    }

    // 6. 分离线程
    if (need_detach) {
        m_jvm->DetachCurrentThread();
    }
}

// 创建Java的IntMsg对象（仅int字段）
jobject update_demo::create_int_msg_obj(JNIEnv* env, int int_value) {
    // 获取IntMsg的构造方法（参数：int，返回void）
    jmethodID msg_constructor = env->GetMethodID(
            m_java_msg_class,
            "<init>", // 构造方法固定名
            "(I)V"    // 签名：int参数，无返回值
    );
    if (!msg_constructor) {
        syslog(LOG_ERR, "JNI: Get IntMsg constructor failed!");
        return nullptr;
    }
    // 创建并返回IntMsg对象
    return env->NewObject(m_java_msg_class, msg_constructor, int_value);
}