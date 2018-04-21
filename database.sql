-- we don't know how to generate database bangumi_note (class Database) :(
create table if not exists optional
(
	id serial not null
		constraint optional_pkey
			primary key,
	allow_register boolean default false not null
)
;

create table if not exists tag
(
	id serial not null
		constraint tag_pkey
			primary key,
	uid integer not null,
	name varchar(8) not null,
	description varchar(128) not null,
	parent integer
		constraint tag_tag_id_fk
			references tag
				on delete set null,
	user_id varchar(16) not null,
	create_time timestamp not null,
	update_time timestamp not null
)
;

create table if not exists "user"
(
	id varchar(16) not null
		constraint user_pkey
			primary key,
	name varchar(16) not null,
	password varchar(128) not null,
	is_admin boolean default false not null,
	create_time timestamp default now() not null,
	update_time timestamp default now() not null,
	last_login timestamp default now() not null,
	uid json default '{}'::json not null,
	salt varchar(64) default ''::character varying not null
)
;

create table if not exists author
(
	id serial not null
		constraint author_pkey
			primary key,
	name varchar(32) not null,
	origin_name varchar(32),
	user_id varchar(16) not null
		constraint author___user_id
			references "user"
				on delete cascade,
	create_time timestamp default now() not null,
	update_time timestamp default now() not null,
	uid integer not null
)
;

create table if not exists company
(
	id serial not null
		constraint company_pkey
			primary key,
	name varchar(32) not null,
	origin_name varchar(32),
	user_id varchar(16) not null
		constraint company___user_id
			references "user"
				on delete cascade,
	create_time timestamp default now() not null,
	update_time timestamp default now() not null,
	uid integer not null
)
;

create table if not exists message
(
	id serial not null
		constraint message_pkey
			primary key,
	uid integer not null,
	type varchar(16) not null,
	content json default '{}'::json not null,
	user_id varchar(16) not null
		constraint message_user_id_fk
			references "user"
				on delete cascade,
	create_time timestamp not null,
	update_time timestamp not null,
	have_read boolean not null
)
;

create table if not exists series
(
	id serial not null
		constraint series_pkey
			primary key,
	name varchar(32) not null,
	user_id varchar(16) not null
		constraint series___user_id
			references "user"
				on delete cascade,
	create_time timestamp default now() not null,
	update_time timestamp default now() not null,
	uid integer not null
)
;

create table if not exists anime
(
	id serial not null
		constraint anime_pkey
			primary key,
	name varchar(128) not null,
	origin_name varchar(128),
	type varchar(12) not null,
	keyword varchar(128),
	series_id integer
		constraint anime___series_id
			references series
				on delete set null,
	user_id varchar(16) not null
		constraint anime___user_id
			references "user"
				on delete cascade,
	uid integer not null,
	create_time timestamp not null,
	update_time timestamp not null,
	score_like double precision,
	score_patient double precision,
	make_make double precision,
	make_drama double precision,
	make_music double precision,
	make_person double precision,
	make_background double precision,
	level_r18 double precision,
	level_r18g double precision,
	other_name varchar(128)
)
;

create table if not exists author_to_anime
(
	id serial not null
		constraint author_to_anime_pkey
			primary key,
	author_id integer not null
		constraint author_to_anime___author_id
			references author
				on delete cascade,
	anime_id integer not null
		constraint author_to_anime___anime_id
			references anime
				on delete cascade
)
;

create table if not exists bangumi
(
	id serial not null
		constraint bangumi_pkey
			primary key,
	uid integer not null,
	name varchar(128) not null,
	serial integer not null,
	anime_id integer not null
		constraint bangumi_anime_id_fk
			references anime
				on delete cascade,
	publish_time timestamp,
	play_type varchar(12) not null,
	play_length integer,
	play_quantity integer,
	finished_time timestamp,
	watching boolean not null,
	multiple_time boolean not null,
	seen_the_original boolean not null,
	user_id varchar(16) not null
		constraint bangumi_user_id_fk
			references "user"
				on delete cascade,
	create_time timestamp not null,
	update_time timestamp not null,
	score_like double precision,
	score_patient double precision,
	make_make double precision,
	make_drama double precision,
	make_music double precision,
	make_person double precision,
	make_background double precision,
	level_r18 double precision,
	level_r18g double precision
)
;

create table if not exists company_to_bangumi
(
	id serial not null
		constraint company_to_bangumi_pkey
			primary key,
	company_id integer not null
		constraint company_to_bangumi_company_id_fk
			references company
				on delete cascade,
	bangumi_id integer not null
		constraint company_to_bangumi_bangumi_id_fk
			references bangumi
				on delete cascade
)
;

create table if not exists diary
(
	id serial not null
		constraint diary_pkey
			primary key,
	uid integer not null,
	bangumi_id integer not null
		constraint diary_bangumi_id_fk
			references bangumi
				on delete cascade,
	name varchar(128) not null,
	total_episode integer not null,
	publish_episode integer not null,
	finished_episode integer not null,
	is_completed boolean not null,
	publish_plan varchar(64) [] not null,
	user_id varchar(16) not null
		constraint diary_user_id_fk
			references "user"
				on delete cascade,
	create_time timestamp not null,
	update_time timestamp not null
)
;

create unique index if not exists diary_bangumi_id_uindex
	on diary (bangumi_id)
;

create table if not exists episode
(
	id serial not null
		constraint episode_pkey
			primary key,
	uid integer not null,
	bangumi_id integer not null
		constraint episode_bangumi_id_fk
			references bangumi
				on delete cascade,
	serial integer not null,
	name varchar(128) not null,
	publish_time timestamp,
	finished_time timestamp,
	user_id varchar(16) not null
		constraint episode_user_id_fk
			references "user"
				on delete cascade,
	create_time timestamp not null,
	update_time timestamp not null
)
;

create table if not exists tag_to_anime
(
	id serial not null
		constraint tag_to_anime_pkey
			primary key,
	tag_id integer not null
		constraint tag_to_anime_tag_id_fk
			references tag
				on delete cascade,
	anime_id integer not null
		constraint tag_to_anime_anime_id_fk
			references anime
				on delete cascade
)
;

create table if not exists tag_to_bangumi
(
	id serial not null
		constraint tag_to_bangumi_pkey
			primary key,
	bangumi_id integer not null
		constraint tag_to_bangumi_bangumi_id_fk
			references bangumi
				on delete cascade,
	tag_id integer not null
		constraint tag_to_bangumi_tag_id_fk
			references tag
				on delete cascade
)
;

