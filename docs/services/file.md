# 파일 서비스

## 책임

상품 이미지, 사업자 증빙 등 업로드 파일의 접근 제어와 메타데이터 관리 담당

## Controller

```text
POST /api/files/upload-urls
GET  /api/files/{fileId}
```

## 핵심 규칙

- 파일 본문은 object storage, DB에는 파일 key·크기·형식·소유자만 저장
- 업로드는 제한된 형식·크기·만료 시간을 가진 presigned URL 방식 우선
- 사업자 증빙 파일은 본인·운영자만 접근 가능하고 조회 이력 보관
- 악성 파일 검사와 공개 이미지·비공개 증빙 파일의 저장소 정책 분리
