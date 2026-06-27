package com.mri.image.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PatientImageAccessMapper {
    @Select("""
            SELECT s.id
            FROM mri_study s
            JOIN mri_exam_order e ON e.id = s.exam_order_id AND e.deleted = 0
            JOIN patient p ON p.id = e.patient_id AND p.deleted = 0
            WHERE p.account_username = #{username}
              AND s.deleted = 0
            ORDER BY s.id DESC
            """)
    List<Long> findStudyIdsByUsername(String username);

    @Select("""
            SELECT COUNT(*) > 0
            FROM mri_study s
            JOIN mri_exam_order e ON e.id = s.exam_order_id AND e.deleted = 0
            JOIN patient p ON p.id = e.patient_id AND p.deleted = 0
            WHERE s.id = #{studyId}
              AND p.account_username = #{username}
              AND s.deleted = 0
            """)
    boolean ownsStudy(@Param("studyId") Long studyId, @Param("username") String username);

    @Select("""
            SELECT COUNT(*) > 0
            FROM mri_report r
            JOIN mri_study s ON s.id = r.study_id AND s.deleted = 0
            JOIN mri_exam_order e ON e.id = s.exam_order_id AND e.deleted = 0
            JOIN patient p ON p.id = e.patient_id AND p.deleted = 0
            WHERE s.id = #{studyId}
              AND p.account_username = #{username}
              AND r.status = 'PUBLISHED'
              AND r.deleted = 0
            """)
    boolean isReportPublished(@Param("studyId") Long studyId, @Param("username") String username);

    @Select("""
            SELECT s.id
            FROM mri_image_file f
            JOIN mri_series se ON se.id = f.series_id AND se.deleted = 0
            JOIN mri_study s ON s.id = se.study_id AND s.deleted = 0
            JOIN mri_exam_order e ON e.id = s.exam_order_id AND e.deleted = 0
            JOIN patient p ON p.id = e.patient_id AND p.deleted = 0
            WHERE f.id = #{fileId}
              AND p.account_username = #{username}
              AND f.deleted = 0
            LIMIT 1
            """)
    Long findStudyIdByFile(@Param("fileId") Long fileId, @Param("username") String username);
}
