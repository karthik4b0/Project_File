public interface BulkUploadDocumentsRepository extends JpaRepository<BulkUploadDocuments, Long> {

    // Fetching only the document IDs for the specified BulkUploadId
    @Query("SELECT bd.documentId FROM BulkUploadDocuments bd WHERE bd.bulkUploadId = :bulkUploadId")
    List<Long> findDocumentIdsByBulkUploadId(@Param("bulkUploadId") Long bulkUploadId);
}

@Transactional
public BulkUploadResponse deleteBulkUpload(final String authUserId, final String requestTrackingId, final String bulkUploadGuid, DQLRequest dqlRequest) {
    BulkUploadResponse bulkUploadResponse = new BulkUploadResponse();

    // Validate the request and fetch the BulkUploadDto
    validateRequest(authUserId, requestTrackingId, bulkUploadGuid);
    BulkUploadDto bulkUploadDto = getBulkUpload(authUserId, requestTrackingId, bulkUploadGuid);

    // Fetch only document IDs related to the bulk upload
    List<Long> documentIds = bulkUploadDocumentsRepository.findDocumentIdsByBulkUploadId(bulkUploadDto.getBulkUploadId());

    if (!documentIds.isEmpty()) {
        logger.info("Bulk upload data found. Proceeding to delete.");

        // Delete documents using only document IDs
        deleteDocumentsFromAlfresco(authUserId, requestTrackingId, documentIds);
        deleteDocumentsFromDoc(documentIds);

        logger.info("Bulk upload data deleted successfully.");
    }

    // Update the bulk upload table
    updateBulkUpload(bulkUploadDto);
    logger.info("Bulk upload table updated successfully.");

    // Save audit data
    bulkUploadAuditService.saveAuditDataForBulkUploadDelete(getBulkUploadFromDto(bulkUploadDto), null,
            AuditActionTypeEnum.BULK_UPLOAD_DELETED.name(), dqlRequest);

    return bulkUploadResponse;
}

private void deleteDocumentsFromAlfresco(final String authUserId, final String requestTrackingId, final List<Long> documentIds) {
    if (!documentIds.isEmpty()) {
        documentIds.forEach(documentId -> {
            // Perform deletion on Alfresco using the document ID
            alfrescoDeleteService.deleteFileOnAlfresco(authUserId, requestTrackingId, documentId.toString());
        });
    }
}

private void deleteDocumentsFromDoc(final List<Long> documentIds) {
    if (!documentIds.isEmpty()) {
        // Split the document IDs into smaller batches for deletion
        Map<String, List<Long>> documentsList = CollectionUtils.splitCollection(documentIds, "ids", SQL_SERVER_MAX_IN_CLAUSE_PARAMS_COUNT);
        for (List<Long> documents : documentsList.values()) {
            // Delete documents from the database using document IDs
            documentRepository.deleteDocumentWithAlfrescoByDocumentIds(documents);
        }
    }
}



