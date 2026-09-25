-- 오늘 뭐 입지 초기 스키마 (deliverables/*-DB.dbml 기준)
-- enum은 PostgreSQL enum 타입 대신 varchar + CHECK로 표현한다.

CREATE TABLE users (
    id          bigserial PRIMARY KEY,
    email       varchar(100) NOT NULL UNIQUE,
    password    varchar(255) NOT NULL,
    name        varchar(30)  NOT NULL,
    is_demo     boolean      NOT NULL DEFAULT false,
    created_at  timestamp    NOT NULL DEFAULT now(),
    updated_at  timestamp
);

CREATE TABLE clothes (
    id               bigserial PRIMARY KEY,
    user_id          bigint       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name             varchar(50)  NOT NULL,
    category         varchar(10)  NOT NULL CHECK (category IN ('TOP', 'BOTTOM', 'SHOES', 'HAT', 'ACC')),
    color            varchar(10)  NOT NULL CHECK (color IN ('블랙', '화이트', '그레이', '베이지', '브라운', '블루', '골드')),
    season           varchar(10)  NOT NULL DEFAULT 'ALL' CHECK (season IN ('SPRING', 'SUMMER', 'FALL', 'WINTER', 'ALL')),
    image_url        varchar(500),
    image_file_name  varchar(255),
    source           varchar(10)  NOT NULL CHECK (source IN ('PHOTO', 'TEXT', 'MANUAL')),
    created_at       timestamp    NOT NULL DEFAULT now(),
    updated_at       timestamp
);
CREATE INDEX idx_clothes_user_category ON clothes (user_id, category);
CREATE INDEX idx_clothes_user_created ON clothes (user_id, created_at);

CREATE TABLE outfits (
    id            bigserial PRIMARY KEY,
    user_id       bigint       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name          varchar(50)  NOT NULL,
    memo          varchar(500),
    source        varchar(10)  NOT NULL CHECK (source IN ('AI', 'MANUAL', 'RANDOM', 'CHALLENGE')),
    request_text  varchar(500),
    ai_reason     text,
    ai_score      integer CHECK (ai_score BETWEEN 0 AND 100),
    ai_comment    varchar(500),
    created_at    timestamp    NOT NULL DEFAULT now(),
    updated_at    timestamp
);
CREATE INDEX idx_outfits_user_created ON outfits (user_id, created_at);

CREATE TABLE outfit_items (
    id           bigserial PRIMARY KEY,
    outfit_id    bigint  NOT NULL REFERENCES outfits (id) ON DELETE CASCADE,
    clothing_id  bigint  NOT NULL REFERENCES clothes (id) ON DELETE CASCADE,
    item_order   integer NOT NULL CHECK (item_order >= 1),
    CONSTRAINT uq_outfit_items_clothing UNIQUE (outfit_id, clothing_id),
    CONSTRAINT uq_outfit_items_order UNIQUE (outfit_id, item_order)
);
-- clothing_id 쪽 CASCADE 삭제 시 전체 스캔을 피하기 위한 인덱스
CREATE INDEX idx_outfit_items_clothing ON outfit_items (clothing_id);

CREATE TABLE outfit_ai_tags (
    id         bigserial PRIMARY KEY,
    outfit_id  bigint      NOT NULL REFERENCES outfits (id) ON DELETE CASCADE,
    tag        varchar(30) NOT NULL,
    CONSTRAINT uq_outfit_ai_tags UNIQUE (outfit_id, tag)
);

CREATE TABLE schedules (
    id          bigserial PRIMARY KEY,
    user_id     bigint    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_date   date      NOT NULL,
    outfit_id   bigint    NOT NULL REFERENCES outfits (id) ON DELETE CASCADE,
    created_at  timestamp NOT NULL DEFAULT now(),
    updated_at  timestamp,
    CONSTRAINT uq_schedules_user_date UNIQUE (user_id, plan_date)
);
CREATE INDEX idx_schedules_outfit ON schedules (outfit_id);

CREATE TABLE ai_requests (
    id          bigserial PRIMARY KEY,
    user_id     bigint      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        varchar(20) NOT NULL CHECK (type IN ('PHOTO_ANALYSIS', 'TEXT_PARSE', 'OUTFIT_RECOMMEND', 'WEEKLY_RECOMMEND', 'CHALLENGE_EVALUATE')),
    status      varchar(10) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED')),
    latency_ms  integer,
    created_at  timestamp   NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_requests_user_created ON ai_requests (user_id, created_at);
