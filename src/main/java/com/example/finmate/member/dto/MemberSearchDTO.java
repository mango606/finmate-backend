package com.example.finmate.member.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class MemberSearchDTO {
    @Pattern(regexp = "^(USER_ID|EMAIL|NAME|PHONE)$",
            message = "올바른 검색 유형을 선택해주세요")
    private String searchType;

    @Size(min = 1, max = 100, message = "검색어는 1-100자 사이여야 합니다")
    private String searchValue;

    @Pattern(regexp = "^(ALL|ACTIVE|INACTIVE)$",
            message = "올바른 상태를 선택해주세요")
    private String status = "ALL";

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private Integer page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private Integer size = 10;

    @Pattern(regexp = "^(REG_DATE|UPDATE_DATE|USER_ID|USER_NAME)$",
            message = "올바른 정렬 기준을 선택해주세요")
    private String sortBy = "REG_DATE";

    @Pattern(regexp = "^(ASC|DESC)$",
            message = "올바른 정렬 방향을 선택해주세요")
    private String sortDirection = "DESC";
}