package com.heerkirov.bangumi.model

import com.heerkirov.bangumi.model.base.ArrayType
import com.heerkirov.bangumi.model.base.Model
import com.heerkirov.bangumi.model.base.ModelInterface
import com.heerkirov.bangumi.model.base.UBModel
import org.hibernate.annotations.Cascade
import org.hibernate.annotations.CascadeType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.util.*
import javax.persistence.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

@Entity @Table(name = "public.series")
class Series(
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="SERIES_ID_SEQ")
        @SequenceGenerator(name="SERIES_ID_SEQ", sequenceName="series_id_seq", allocationSize = 1)
        @Column(name= "id")var id: Int? = null,
        @Column(name = "name", length = 32, nullable = false)var name: String = "",

        @Column(name = "uid", nullable = false)var uid: Int? = null,
        @Column(name = "user_id", nullable = false) var userId: String? = null,
        @Column(name = "create_time", nullable = false)var createTime: Calendar? = null,
        @Column(name = "update_time", nullable = false)var updateTime: Calendar? = null,

        @OneToMany@JoinColumn(name = "series_id") var animeList: Set<Anime> = HashSet()
): UBModel()

@Entity @Table(name = "public.author")
class Author(
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="AUTHOR_ID_SEQ")
        @SequenceGenerator(name="AUTHOR_ID_SEQ", sequenceName="author_id_seq", allocationSize = 1)
        @Column(name = "id")var id: Int? = null,
        @Column(name = "uid", nullable = false)var uid: Int? = null,
        @Column(name = "name", length = 32, nullable = false)var name: String = "",
        @Column(name = "origin_name", length = 32)var originName: String? = null,

        @Column(name = "user_id", nullable = false) var userId: String? = null,
        @Column(name = "create_time", nullable = false)var createTime: Calendar? = null,
        @Column(name = "update_time", nullable = false)var updateTime: Calendar? = null,

        @ManyToMany(targetEntity = Anime::class, mappedBy = "authorList", fetch = FetchType.EAGER) var AnimeList: Set<Anime> = HashSet()
): UBModel()

@Entity @Table(name = "public.company")
class Company(
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="COMPANY_ID_SEQ")
        @SequenceGenerator(name="COMPANY_ID_SEQ", sequenceName="company_id_seq", allocationSize = 1)
        @Column(name = "id")var id: Int? = null,
        @Column(name = "uid", nullable = false)var uid: Int? = null,
        @Column(name = "name", length = 32, nullable = false)var name: String = "",
        @Column(name = "origin_name", length = 32)var originName: String? = null,

        @Column(name = "user_id", nullable = false) var userId: String? = null,
        @Column(name = "create_time", nullable = false)var createTime: Calendar? = null,
        @Column(name = "update_time", nullable = false)var updateTime: Calendar? = null
): UBModel()

@Entity @Table(name = "public.anime")
@TypeDef(name = "array", typeClass = ArrayType::class)
class Anime constructor(
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="ANIME_ID_SEQ")
        @SequenceGenerator(name="ANIME_ID_SEQ", sequenceName="anime_id_seq", allocationSize = 1)
        @Column(name = "id")var id: Int? = null,
        @Column(name = "uid", nullable = false)var uid: Int? = null,
        @Column(name = "name", nullable = false, length = 128)var name: String = "",
        @Column(name = "other_name", length = 128)var otherName: String? = null,
        @Column(name = "origin_name", length = 128)var originName: String? = null,
        @Column(name = "type", length = 12, nullable = false)var type: String = "other", //原作类型。[小说/漫画/游戏/原创/其他][novel/comic/game/origin/other]
        @Column(name = "keyword", length = 128)var keyword: String? = null,
        @ManyToOne@JoinColumn(name = "series_id")var series: Series? = null,
        @ManyToMany(targetEntity = Author::class,  fetch = FetchType.EAGER)@JoinTable(name = "public.author_to_anime",
                joinColumns = [JoinColumn(name = "anime_id")],
                inverseJoinColumns = [JoinColumn(name = "author_id")]) var authorList: Set<Author> = HashSet(),

        @Column(name = "user_id", nullable = false) var userId: String? = null,
        @Column(name = "create_time", nullable = false)var createTime: Calendar? = null,
        @Column(name = "update_time", nullable = false)var updateTime: Calendar? = null,

        @Column(name = "score_like")var scoreLike: Double? = null, //喜好评分
        @Column(name = "score_patient")var scorePatient: Double? = null, //耐用性评分

        @Column(name = "make_make")var makeMake: Double? = null, //制作
        @Column(name = "make_drama")var makeDrama: Double? = null, //剧本
        @Column(name = "make_music")var makeMusic: Double? = null, //音乐
        @Column(name = "make_person")var makePerson: Double? = null, //人物
        @Column(name = "make_background")var makeBackground: Double? = null, //背景

        @Column(name = "level_r18")var levelR18: Double? = null, //R18评级
        @Column(name = "level_r18g")var levelR18G: Double? = null //R18G评级
): UBModel()

class Bangumi(
        var id: Int,
        var name: String,
        var number: Int,
        var anime_id: Int,
        var publish_time: Date,
        var play_type: String,
        var play_length: Int,
        var play_count: Int,
        //var companies

        var finished_time: Date,
        var watching: Boolean,
        var double: Boolean,
        var origin: Boolean,

        var user_id: String
)

class Episode (
        var id: Int,
        var bangumi_id: Int,
        var number: Int,
        var name: String,
        var publish_time: Date,
        var finished_time: Date,

        var user_id: String
)

class Tag(
        var id: Int,
        var name: String,
        var description: String,
        var parent: Int?,

        var user_id: String
)

@Entity @Table(name = "public.author_to_anime")
class AuthorToAnime(
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="AUTHOR_TO_ANIME_ID_SEQ")
        @SequenceGenerator(name="AUTHOR_TO_ANIME_ID_SEQ", sequenceName="author_to_anime_id_seq", allocationSize = 1)
        @Column(name = "id")var id: Int? = null,
        @Column(name = "author_id", nullable = false)var authorId: Int? = null,
        @Column(name = "anime_id", nullable = false)var animeId: Int? = null
): Model()

class CompanyToBangumi(
        var id: Int,
        var company_id: Int,
        var bangumi_id: Int
)

class TagToCommit(
        var id: Int,
        var commit_id: Int,
        var tag_id: Int
)