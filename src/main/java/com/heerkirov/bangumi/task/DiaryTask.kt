package com.heerkirov.bangumi.task

import com.heerkirov.bangumi.service.DiaryService
import com.heerkirov.bangumi.service.MessageService
import org.hibernate.HibernateException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DiaryTask(@Autowired private val diaryService: DiaryService,
                @Autowired private val messageService: MessageService) {
    @Scheduled(cron = "0 0/15 * * * ?")
    fun publishEpisodeUpdate() {
        //该任务将处理所有diary项目中的plan。
        try{
            //执行任务计划
            val results = diaryService.analysisPlan()
            //发布消息
            messageService.publishDiaryPublish(results)
        }catch(e: HibernateException) {
            e.printStackTrace()
        }
    }
}