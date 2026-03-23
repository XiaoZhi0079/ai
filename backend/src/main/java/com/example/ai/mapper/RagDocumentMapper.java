package com.example.ai.mapper;

import com.example.ai.pojo.RagDocumentInfo;
import com.example.ai.pojo.RagDocumentDetail;
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
            INSERT INTO rag_documents (file_name, oss_url, uploaded_by, owner_user_id, knowledge_scope, chunk_count, extracted_text)
            VALUES (#{fileName}, #{ossUrl}, #{uploadedBy}, #{ownerUserId}, #{knowledgeScope}, #{chunkCount}, #{extractedText})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(RagDocumentInfo document);

    @Select("""
            SELECT d.id,
                   d.file_name AS fileName,
                   d.oss_url AS ossUrl,
                   d.uploaded_by AS uploadedBy,
                   uploader.username AS uploadedByName,
                   d.owner_user_id AS ownerUserId,
                   owner.username AS ownerUserName,
                   d.knowledge_scope AS knowledgeScope,
                   d.chunk_count AS chunkCount,
                   d.created_at AS createdAt,
                   d.updated_at AS updatedAt
            FROM rag_documents d
            LEFT JOIN users uploader ON d.uploaded_by = uploader.id
            LEFT JOIN users owner ON d.owner_user_id = owner.id
            WHERE d.knowledge_scope = 'PUBLIC'
               OR d.owner_user_id = #{userId}
            ORDER BY d.id DESC
            """)
    List<RagDocumentInfo> selectVisibleDocuments(@Param("userId") Integer userId);

    @Select("""
            SELECT d.id,
                   d.file_name AS fileName,
                   d.owner_user_id AS ownerUserId,
                   d.knowledge_scope AS knowledgeScope,
                   d.extracted_text AS extractedText
            FROM rag_documents d
            WHERE d.knowledge_scope = 'PUBLIC'
               OR d.owner_user_id = #{userId}
            ORDER BY d.id DESC
            """)
    List<RagDocumentInfo> selectVisibleDocumentsForSearch(@Param("userId") Integer userId);

    @Select("""
            SELECT d.id,
                   d.file_name AS fileName,
                   d.oss_url AS ossUrl,
                   d.uploaded_by AS uploadedBy,
                   uploader.username AS uploadedByName,
                   d.owner_user_id AS ownerUserId,
                   owner.username AS ownerUserName,
                   d.knowledge_scope AS knowledgeScope,
                   d.chunk_count AS chunkCount,
                   d.created_at AS createdAt,
                   d.updated_at AS updatedAt
            FROM rag_documents d
            LEFT JOIN users uploader ON d.uploaded_by = uploader.id
            LEFT JOIN users owner ON d.owner_user_id = owner.id
            WHERE d.id = #{id}
            """)
    RagDocumentInfo selectById(@Param("id") Integer id);

    @Select("""
            SELECT d.id,
                   d.file_name AS fileName,
                   d.oss_url AS ossUrl,
                   d.uploaded_by AS uploadedBy,
                   uploader.username AS uploadedByName,
                   d.owner_user_id AS ownerUserId,
                   owner.username AS ownerUserName,
                   d.knowledge_scope AS knowledgeScope,
                   d.chunk_count AS chunkCount,
                   d.extracted_text AS extractedText,
                   d.created_at AS createdAt,
                   d.updated_at AS updatedAt
            FROM rag_documents d
            LEFT JOIN users uploader ON d.uploaded_by = uploader.id
            LEFT JOIN users owner ON d.owner_user_id = owner.id
            WHERE d.id = #{id}
            """)
    RagDocumentDetail selectDetailById(@Param("id") Integer id);

    @org.apache.ibatis.annotations.Update("""
            UPDATE rag_documents
            SET chunk_count = #{chunkCount},
                extracted_text = #{extractedText},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int updateContent(@Param("id") Integer id,
                      @Param("extractedText") String extractedText,
                      @Param("chunkCount") Integer chunkCount);

    @org.apache.ibatis.annotations.Update("""
            UPDATE rag_documents
            SET file_name = #{fileName},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int updateFileName(@Param("id") Integer id,
                       @Param("fileName") String fileName);

    @Delete("DELETE FROM rag_documents WHERE id = #{id}")
    int deleteById(Integer id);
}
