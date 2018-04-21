package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.model.*
import org.springframework.stereotype.Service

@Service
interface OptionalService {
    var allowRegister: Boolean
}

@Service
interface MessageService {
    fun existAnyMessage(user: User): Boolean

    fun unreadAndSetMessages(user: User): List<Message>

    fun publishGeneral(lists: List<GeneralInfo>): List<Message>

    fun publishDiaryPublish(lists: List<DiaryPublishInfo>): List<Message>

    class GeneralInfo(val user: User, val title: String, val content: String)
    class DiaryPublishInfo(val diary: Diary, val oldCount: Int, val newCount: Int)
}

@Service
interface UserService : RestfulService<User>

@Service
interface SeriesService : RestfulService<Series>

@Service
interface CompanyService : RestfulService<Company>

@Service
interface AuthorService: RestfulService<Author>

@Service
interface AnimeService : RestfulService<Anime>

@Service
interface BangumiService: RestfulService<Bangumi>

@Service
interface EpisodeService: RestfulService<Episode>

@Service
interface TagService: RestfulService<Tag>

@Service
interface DiaryService: RestfulService<Diary> {
    fun analysisPlan(): List<MessageService.DiaryPublishInfo>

    fun handleFinished(obj: Diary): Diary

}

