package com.zealsinger.interview_butler.cache

import EventDataManager
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class EmailCache {
    companion object {
        // <邮箱接收时间 邮箱实体>
        private val emailCache = ConcurrentHashMap<LocalDate,Set<Event>>()
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        fun addCache(content:String): Boolean {
            val newEvents = EventDataManager.parseFromJson(content)
            newEvents.forEach { event ->
                emailCache[event.emailReceiveTime.toLocalDate()] = emailCache[event.emailReceiveTime.toLocalDate()]?.plus(event) ?: setOf(event)
            }
            return true
        }

        fun getCache(startDate: LocalDate, endDate: LocalDate): Set<Event> {
            val result = mutableSetOf<Event>()
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate)

            for (day in 0..daysBetween) {
                val date: LocalDate? = startDate.plusDays(day)
                val events = emailCache[date]
                if (events != null) {
                    result.addAll(events)
                }
            }
            return result
        }

    }
}


/*  旧方案 缓存结构为<<批次最早时间，批次最晚时间> 批次邮箱内容> 区间合并比较麻烦 需要重新设计
class EmailCache {
    companion object {
        // <<批次最早时间，批次最晚时间> 批次邮箱内容>
        private val emailCache = ConcurrentHashMap<Pair<LocalDateTime,LocalDateTime>, Set<Event>>()
        val executor = Executors.newSingleThreadExecutor()
        fun addCache(content:String): Boolean {
            val newEvents = EventDataManager.parseFromJson(content).events
            // TODO Map遍历过程中不能进行删除和添加 容易报报并发错误 待解决（可以考虑写时复制方案）
            newEvents.sortedBy { event -> event.emailReceiveTimeStr }.let { newEvents ->
                // 当前批次的子最早时间 和  最晚时间
                val newTime: Pair<LocalDateTime, LocalDateTime> = Pair(newEvents.first().emailReceiveTime, newEvents.last().emailReceiveTime)
                if(emailCache.isNotEmpty()){
                    emailCache.forEach { (key, value) ->
                        val (oldStart, oldEnd) =  key
                        // 当前批次时间起始时间 在 缓存批次起始时间之后 且 当前批次结束时间在缓存批次结束时间之后 整合批次为key-start->now-end
                        // key-start  now-start  key-end now-end
                        if(oldStart.isBefore(newTime.first) && oldEnd.isBefore(newTime.second)){
                            emailCache[key] = value.plus(newEvents)
                        }
                        // 当前批次时间起始时间 在 缓存批次起始时间之前 且 当前批次结束时间在缓存批次结束时间之前 整合批次为now-start->key-end
                        //  now-start key-start  now-end  key-end
                        else if(oldStart.isAfter(newTime.first) && oldEnd.isAfter(newTime.second)){

                        }
                        else if(){

                        }
                    }

                }else{
                    emailCache[newTime] = HashSet(newEvents)
                }
            }

            return false;
        }

    }
}*/
