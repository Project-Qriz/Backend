package com.qriz.sqld.dto.application;

import java.time.LocalDate;
import java.util.List;

import com.qriz.sqld.domain.application.Application;

import lombok.Getter;
import lombok.Setter;

public class ApplicationRespDto {
    
    @Getter
    @Setter
    public static class ApplyListRespDto {
        private List<ApplicationDetail> applications;

        public ApplyListRespDto(List<ApplicationDetail> applications) {
            this.applications = applications;
        }

        @Getter
        @Setter
        public static class ApplicationDetail {
            private Long id;
            private LocalDate date;
            private String testTime;
            private String location;

            public ApplicationDetail(Application application) {
                this.id = application.getId();
                this.date = application.getDate();
                this.testTime = application.getTestTime();
                this.location = application.getLocation();
            }
        }
    }

    @Getter
    @Setter
    public static class ApplyRespDto {
        private Long applyId;
        private String date;
        private String testTime;
        private String location;

        public ApplyRespDto(Long applyId, String date, String testTime, String location) {
            this.applyId = applyId;
            this.date = date;
            this.testTime = testTime;
            this.location = location;
        }
    }
}
