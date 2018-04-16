package com.heerkirov.bangumi.controller.ktml

import com.heerkirov.ktml.builder.ConstProxy
import org.springframework.stereotype.Component

@Component
class Proxy : ConstProxy({
    dim("val_logo", "番组笔记")
    //web login系列地址
    url("web_login", {"/web/login"})
    url("web_register", {"/web/register"})
    //home系列地址
    url("web_home", {"/web/home"})
    //diary系列地址
    url("web_diary", {"/web/diary"})
    //data系列地址
    url("web_data", {"/web/data"})
    url("web_data_series_list", {"/web/data/series"})
    url("web_data_series_create", {"/web/data/series/create"})
    url("web_data_series_detail", {"/web/data/series/info/${it["id"]?:""}"})
    url("web_data_author_list", {"/web/data/authors"})
    url("web_data_author_create", {"/web/data/authors/create"})
    url("web_data_author_detail", {"/web/data/authors/info/${it["id"]?:""}"})
    url("web_data_company_list", {"/web/data/companies"})
    url("web_data_company_create", {"/web/data/companies/create"})
    url("web_data_company_detail", {"/web/data/companies/info/${it["id"]?:""}"})
    url("web_data_anime_list", {"/web/data/animes"})
    url("web_data_anime_create", {"/web/data/animes/create"})
    url("web_data_anime_detail", {"/web/data/animes/info/${it["id"]?:""}"})
    url("web_data_bangumi_list", {"/web/data/bangumis"})
    url("web_data_bangumi_create", {"/web/data/bangumis/create"})
    url("web_data_bangumi_detail", {"/web/data/bangumis/info/${it["id"]?:""}"})
    url("web_data_tag_list", {"/web/data/tags"})
    url("web_data_tag_create", {"/web/data/tags/create"})
    url("web_data_tag_detail", {"/web/data/tags/info/${it["id"]?:""}"})
    //statistics系列地址
    url("web_statistics", {"/web/statistics"})


    //user系列api
    url("api_user_login", {"/api/user/login.json"})
    url("api_user_logout", {"/api/user/logout.json"})
    url("api_user_register", {"/api/user/register.json"})
    //content系列api
    url("api_content_series", {"/api/content/series.json"})
    url("api_content_series_detail", {"/api/content/series/${it["id"]?:""}.json"})
    url("api_content_author", {"/api/content/authors.json"})
    url("api_content_author_detail", {"/api/content/authors/${it["id"]?:""}.json"})
    url("api_content_company", {"/api/content/companies.json"})
    url("api_content_company_detail", {"/api/content/companies/${it["id"]?:""}.json"})
    url("api_content_anime", {"/api/content/animes.json"})
    url("api_content_anime_detail", {"/api/content/animes/${it["id"]?:""}.json"})
    url("api_content_bangumi", {"/api/content/bangumis.json"})
    url("api_content_bangumi_detail", {"/api/content/bangumis/${it["id"]?:""}.json"})
    url("api_content_tag", {"/api/content/tags.json"})
    url("api_content_tag_detail", {"/api/content/tags/${it["id"]?:""}.json"})
})