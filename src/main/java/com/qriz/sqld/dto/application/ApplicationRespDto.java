package com.qriz.sqld.dto.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

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
            private Long applyId;
            private String period;
            private String date;
            private String testTime;

            public ApplicationDetail(Application application) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM.dd(E)");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                DateTimeFormatter testDateFormatter = DateTimeFormatter.ofPattern("M월 d일(E)");
        
                this.applyId = application.getId();
                this.period = application.getStartDate().format(dateFormatter) + " ~ " + application.getEndDate().format(dateFormatter);
                this.date = application.getExamDate().format(testDateFormatter);
                this.testTime = application.getTestTime();
            }
        }
    }

    @Getter
    @Setter
    public static class ApplyRespDto {
        private Long applyId;
        private String period;
        private String examDate;
        private String testTime;

        public ApplyRespDto(Long applyId, String period, String examDate, String testTime) {
            this.applyId = applyId;
            this.period = period;
            this.examDate = examDate;
            this.testTime = testTime;
        }
    }

    @Getter
    @Setter
    public static class ExamDDayRespDto {
        private Integer DDay;

        public ExamDDayRespDto(Integer DDay) {
            this.DDay = DDay;
        }
    }
}
