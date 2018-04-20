# web页面设计
## web页面结构
* 导航
    * 主页
    * 追番
    * 数据库
        * 系列(series) REST
            * RETRIEVE 可跳转到按series筛选的anime list
        * 作者(authors) REST
            * RETRIEVE 可跳转到按author筛选的anime list
        * 制作公司(companies) REST
            * RETRIEVE 可跳转到按company筛选的bangumi list
        * 番组(animes) REST
            * 有一个开关控制评价信息的显示。
            * LIST 可跳转到anime的series authors tags
            * RETRIEVE 可跳转到anime的series authors;可跳转到anime下属的bangumi list
            * anime相关api由开关包括评价|标签等相关信息。
        * 番剧(bangumis) REST
            * 有一个开关控制评价信息的显示。
            * LIST 可跳转到bangumi的anime companies tags
            * RETRIEVE 可跳转到bangumi的anime companies
                * 查看下属的episodes的REST
            * anime相关api由开关包括评价|标签等相关信息。
        * 标签库(tags) REST
            * RETRIEVE 可跳转到按tag筛选的anime/bangumi list
    * 统计
        * 数量概览:
            * 记录在册的番组、番剧总数，记录在册的作者、公司、标签总数
            * 看完的番剧总数、正在看的番剧总数
        * 时段统计：
            * 在指定时间段区间内，看完的番剧数量/话数构成的直方图