package com.example.ai.mapper;

import com.example.ai.pojo.RagDocumentInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RagDocumentMapper {

    @Insert("""
            INSERT INTO rag_documents (file_name, oss_url, uploaded_by, owner_user_id, knowledge_scope)
            VALUES (#{fileName}, #{ossUrl}, #{uploadedBy}, #{ownerUserId}, #{knowledgeScope})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(RagDocumentInfo document);

    @Select("""
            SELECT id,
                   file_name AS fileName,
                   oss_url AS ossUrl,
                   uploaded_by AS uploadedBy,
                   owner_user_id AS ownerUserId,
                   knowledge_scope AS knowledgeScope,
                   created_at AS createdAt
            FROM rag_documents
            WHERE knowledge_scope = 'PUBLIC'
               OR owner_user_id = #{userId}
            ORDER BY id DESC
            """)
    List<RagDocumentInfo> selectVisibleDocuments(@Param("userId") Integer userId);

    @Select("""
            SELECT id,
                   file_name AS fileName,
                   oss_url AS ossUrl,
                   uploaded_by AS uploadedBy,
                   owner_user_id AS ownerUserId,
                   knowledge_scope AS knowledgeScope,
                   created_at AS createdAt
            FROM rag_documents
            WHERE id = #{id}
            """)
    RagDocumentInfo selectById(@Param("id") Integer id);

    @Delete("DELETE FROM rag_documents WHERE id = #{id}")
    int deleteById(Integer id);
}
