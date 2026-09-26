-- clothes.image_url에 공개 URL 대신 사진 저장소 키({userId}/{uuid}.{jpg|png})를 저장한다.
-- 저장소(로컬 디스크 → S3 등)나 공개 URL이 바뀌어도 DB 값은 그대로 두기 위함이다.
-- 컬럼명은 명세(DBML)와 맞추려고 유지하고, API 응답의 image_url은 서버가 키로 만들어 준다.
UPDATE clothes
SET image_url = substring(image_url FROM '^/api/images/(.+)$')
WHERE image_url LIKE '/api/images/%';

COMMENT ON COLUMN clothes.image_url IS '사진 저장소 키 {userId}/{uuid}.{jpg|png} (URL 아님). NULL이면 카테고리+색상 대표 사진';
