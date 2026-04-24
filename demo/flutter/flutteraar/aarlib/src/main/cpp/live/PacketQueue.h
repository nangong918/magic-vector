#ifndef PACKET_QUEUE_H
#define PACKET_QUEUE_H

#include <queue>
#include <thread>

/**
 * 线程安全的 RTMP 包队列。
 * @tparam T 队列元素类型（本项目为 RTMPPacket*）
 */
template<typename T>
class PacketQueue {
    typedef void (*ReleaseCallback)(T &);

private:
    std::mutex m_mutex;
    std::condition_variable m_cond;
    std::queue<T> m_queue;
    bool m_running;

    ReleaseCallback releaseCallback;

public:

    /**
     * 推送元素到队列。
     */
    void push(T new_value) {
        std::lock_guard<std::mutex> lock(m_mutex);
        if (m_running) {
            m_queue.push(new_value);
            m_cond.notify_one();
        }
    }

    /**
     * 弹出一个元素。
     */
    int pop(T &value) {
        int ret = 0;
        std::unique_lock<std::mutex> lock(m_mutex);
        if (!m_running) {
            return ret;
        }
        if (!m_queue.empty()) {
            value = m_queue.front();
            m_queue.pop();
            ret = 1;
        }
        return ret;
    }

    /**
     * 清空队列并执行资源释放回调。
     */
    void clear() {
        std::lock_guard<std::mutex> lock(m_mutex);
        int size = m_queue.size();
        for (int i = 0; i < size; ++i) {
            T value = m_queue.front();
            releaseCallback(value);
            m_queue.pop();
        }
    }

    /**
     * 更新队列运行状态。
     */
    void setRunning(bool run) {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_running = run;
    }

    /**
     * 判断队列是否为空。
     */
    bool empty() {
        std::lock_guard<std::mutex> lock(m_mutex);
        return m_queue.empty();
    }

    /**
     * 获取队列长度。
     */
    int size() {
        std::lock_guard<std::mutex> lock(m_mutex);
        return static_cast<int>(m_queue.size());
    }

    /**
     * 设置元素释放回调。
     */
    void setReleaseCallback(ReleaseCallback callback) {
        releaseCallback = callback;
    }

};

#endif // PACKET_QUEUE_H
