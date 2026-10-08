# 파일

## 구현 상태

파일 metadata와 로컬 파일 저장 API를 제공. 기본 저장 경로는 `E:/buyeong_dev/umji-market/data/files`이며 IDC 마운트 경로는 `UMJI_FILE_STORAGE_ROOT`로 지정.<br>
파일 원본은 로컬 저장소에만 두고 DB에는 파일 분류·소유 계정·불투명 저장 key·MIME·크기·상태 등 metadata만 저장.<br>

## 파일 분류와 접근 정책

| 분류 | 저장·조회 범위 | 접근 방식 | 권한 기준 |
| --- | --- | --- | --- |
| 공개 상품 이미지 | 공개 카탈로그에서 노출되는 상품 이미지 | 상품에 연결된 파일만 공개 API 응답으로 제공 | 운영자 `PRODUCT_WRITE` 업로드 권한. 미연결 파일은 비공개 |
| 사업자 증빙 및 동의 증빙 파일 | 비공개 로컬 저장소 | API가 권한 확인 후 파일 응답 제공 | 운영자 `ADMIN_ACCOUNT_MANAGE` 권한 및 요청 대상 계정과 소유 계정 일치 |

파일 key는 추측할 수 없는 값으로 발급하며 클라이언트가 전달한 key나 URL만으로 접근을 허용하지 않음.<br>
공개 이미지와 비공개 증빙은 별도 하위 경로에 저장하고, 저장소 디렉터리는 정적 웹 경로로 노출하지 않음.<br>
상품 이미지 등록·수정은 기존 운영자 카탈로그 permission을 검사하며, 공개 접근은 표시 가능한 상품에 연결된 파일에만 허용.<br>
업로드 token은 단회 사용 및 만료 처리. token 원문과 파일 원본은 DB에 저장하지 않음.<br>

## 업로드 및 파일 접근 흐름

```mermaid
flowchart TD
    Client["앱 / React WebView"] --> Request["POST /api/files/uploads: 분류·소유자·파일명·MIME·크기"]
    Request --> Auth{"인증 및 파일 분류별 권한 확인"}
    Auth -->|실패| Deny["401 / 403 응답"]
    Auth -->|공개 이미지| CatalogScope{"PRODUCT_WRITE 권한 확인"}
    Auth -->|비공개 증빙| AccountScope{"ADMIN_ACCOUNT_MANAGE 및 계정 범위 확인"}
    CatalogScope -->|거부| Deny
    AccountScope -->|거부| Deny
    CatalogScope -->|허용| Validate["허용 MIME·파일 크기·파일명 검증"]
    AccountScope -->|허용| Validate
    Validate -->|실패| Invalid["요청 거부"]
    Validate -->|통과| Metadata["불투명 file ID·저장 key·만료 upload token metadata 저장"]
    Metadata --> UploadUrl["API PUT 업로드 URL 반환"]
    UploadUrl --> Upload["PUT /api/files/{fileId}/content: token·MIME·실제 크기 검사"]
    Upload --> Store["임시 파일 기록 후 원자적 이동"]
    Store --> Complete["metadata 상태 UPLOADED 전환"]
    Complete --> Result["파일 ID·metadata 반환"]
    Result -->|공개 이미지| Public["상품 이미지 연결 이후 공개 응답"]
    Result -->|비공개 증빙| Private["권한·소유자 확인 후 파일 응답"]
```

로컬 저장 경로는 `UMJI_FILE_STORAGE_ROOT` 환경 변수로 주입하며 기본 개발 경로는 `E:/buyeong_dev/umji-market/data/files`.<br>
임시 파일은 검증 완료 후 같은 파일시스템 안에서 원자적으로 저장 경로로 이동.<br>
