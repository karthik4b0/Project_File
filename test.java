@Test
public void testDeleteBulkUpload() {
    // Initialize test data
    String bulkUploadGuid = UUID.randomUUID().toString();
    String authUserId = "skothuri";
    
    // Create and set up the BulkUpload object
    BulkUpload bulkUpload = new BulkUpload();
    bulkUpload.setCreateUserId(authUserId);
    bulkUpload.setBulkUploadGUID(bulkUploadGuid);
    
    // Create a mock BulkUploadDocuments list (now with Document Ids only)
    List<Long> mockDocIdList = new ArrayList<>();
    mockDocIdList.add(1L);  // Simulate a document ID that would be deleted
    
    // Mock the bulkUploadDocumentsRepository to return document IDs instead of full documents
    when(bulkUploadDocumentsRepository.findDocumentIdsByBulkUploadId(anyLong())).thenReturn(mockDocIdList);
    
    // Mock the bulkUploadRepository to return the bulk upload object
    when(bulkUploadRepository.findByBulkUploadGUIDAndPublishFlFalseAndDeleteFlFalse(bulkUploadGuid)).thenReturn(bulkUpload);
    
    // Mock the deletion behavior in the document repository (just a simulation of the deletion)
    when(documentRepository.deleteDocumentWithAlfrescoByDocumentIds(mockDocIdList)).thenReturn(1);
    
    // Mock Alfresco delete service behavior
    doNothing().when(alfrescoDeleteService).deleteFileOnAlfresco(anyString(), anyString(), anyString());
    
    // Mock the audit save behavior
    doNothing().when(bulkUploadAuditService).saveAuditDataForBulkUploadDelete(any(), any(), any(), any());
    
    // Call the service method to test
    BulkUploadResponse bulkUploadResponse = bulkUploadService.deleteBulkUpload(authUserId, "requestTrackingId", bulkUploadGuid, getDQLRequest());
    
    // Assert that the response is not null
    Assert.assertNotNull(bulkUploadResponse);
    
    // Verify the deletion calls
    Mockito.verify(documentRepository, times(1)).deleteDocumentWithAlfrescoByDocumentIds(mockDocIdList);
    Mockito.verify(alfrescoDeleteService, atMost(1)).deleteFileOnAlfresco(any(), any(), any());
    Mockito.verify(bulkUploadAuditService, times(1)).saveAuditDataForBulkUploadDelete(any(), any(), any(), any());
}
