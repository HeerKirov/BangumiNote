package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.model.*
import org.springframework.stereotype.Service

@Service
interface OptionalService {
    var allowRegister: Boolean
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
interface TagService: RestfulService<Tag>