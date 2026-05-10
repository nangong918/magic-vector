#ifndef PACKET_QUEUE_H
#define PACKET_QUEUE_H

#include <queue>
#include <thread>

/**
 * 线程安全的 RTMP 包队列。
 * @tparam T 队列元素类型（本项目为 RTMPPacket*）
 *
 * 【并发模型补充】生产者：AudioStream / VideoStream 经 callback 入队；
 * 消费者：推流线程 start() 中 pop。mutex 保护 queue 与 m_running。
 *
 * 【注意】push() 仅在 m_running==true 时接受数据，用于停止推流时拒绝新包。
 * pop() 若队列空则立即返回 0，发送线程里通过「循环 pop」轮询；
 * 成员里有 condition_variable 但未配合 wait 使用，属于实现遗留。
 *
 * 【可选优化】若关心 CPU，可将 pop 改为 condition_variable::wait，避免忙等。
 */

#include <queue>
#include <mutex>
#include <condition_variable>
#include <thread>

template<typename T>
class PacketQueue {
    typedef void (*ReleaseCallback)(T &);

private:
    std::mutex m_mutex;                  // 保护 m_queue、m_running 以及下文条件变量协作时的临界区
    std::condition_variable m_cond;       // 预留：可与 wait 配合做阻塞 pop（当前实现未阻塞等待）
    std::queue<T> m_queue;               // FIFO：编码线程 push，推流线程 pop
    bool m_running;                      // false 时 push 拒绝入队、pop 返回 0，用于 stop/release

    ReleaseCallback releaseCallback;    // clear() 时对队列残留元素调用，释放 RTMPPacket*

public:

    /**
     * 推送元素到队列。
     * @note 非运行状态下不入队，避免 stop 后仍有滞后编码线程投递。
     */
    void push(T new_value) {
        std::lock_guard<std::mutex> lock(m_mutex); // 进入临界区：与其它成员函数串行
        if (m_running) {
            m_queue.push(new_value);       // 尾插 RTMPPacket*
            m_cond.notify_one();           // 唤醒可能在等待的消费者（若未来改为阻塞 wait）
        }
    }

    /**
     * 弹出一个元素。
     * @return 1 表示取到；0 表示队列为空或未运行（非阻塞）。
     */
    int pop(T &value) {
        int ret = 0;
        std::unique_lock<std::mutex> lock(m_mutex); // unique_lock：预留将来配合 condition_variable
        if (!m_running) {
            return ret;                    // 已停止：不再消费，避免半关闭状态误读数据
        }
        if (!m_queue.empty()) {
            value = m_queue.front();       // 先进先出：保证音视频交错顺序由入队顺序体现
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
            releaseCallback(value);        // 默认指向 releasePackets：RTMPPacket_Free + delete
            m_queue.pop();
        }
    }

    /**
     * 更新队列运行状态。
     */
    void setRunning(bool run) {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_running = run;                   // start() 后置 true 允许 push；stop/release 后置 false 掐断入队
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
        releaseCallback = callback;       // 由 RtmpPusher::native_1init 设为 releasePackets
    }

};

#endif // PACKET_QUEUE_H
