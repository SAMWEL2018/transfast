package com.sla.matercard.inbound.Repository;

import com.sla.matercard.inbound.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author samwel.wafula
 * Created on 18/06/2024
 * Time 11:40
 * Project Transfast
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    @Query(nativeQuery = true, value = "select * from tbl_transactions where transaction_status in (:status)")
    List<Invoice> getTransactionsByStatusToProcess(@Param("status") String... status);

    @Query(nativeQuery =  true,value = "SELECT * from tbl_transactions where tf_pin=:tfpin")
    Invoice getInvoiceByRef(@Param("tfpin") String tfPin);

    @Query(nativeQuery = true, value = """
            select * from tbl_transactions where transaction_status in (:status) and partner_process=:partner_process
            """)
    List<Invoice> getTransactionsByStatusAndPartnerProcessToProcess(@Param("partner_process") String partnerProcess, @Param("status") String... status);

    @Query(nativeQuery = true, value = """
            update tbl_transactions set transaction_status=:transaction_status,switch_response=:switch_response,
            incident_code=:incident_code,incident_description=:incident_description,datetime_modified=:dateTimeModified
             where tf_pin=:tfPin
            """)
    @Modifying
    @Transactional
    void updateTransactionFromFinalBridgeResponse(@Param("transaction_status") String transactionStatus,
                                                  @Param("switch_response") String switchResponse,
                                                  @Param("incident_code") String incidentCode,
                                                  @Param("incident_description") String incidentDescription,
                                                  @Param("tfPin") String tfPin,
                                                  @Param("dateTimeModified") Timestamp dateTimeModified
    );

    @Query(nativeQuery = true, value = "update tbl_transactions set transaction_status=:status where tf_pin=:tfPin")
    @Modifying
    @Transactional
    void updateTrnStatus(@Param("tfPin") String tfPin, @Param("status") String status);
    @Query(nativeQuery = true, value = "update tbl_transactions set transaction_status=:status,partner_process=:partner_status where tf_pin=:tfPin")
    @Modifying
    @Transactional
    void updateTrnStatusAndPartnerStatus(@Param("tfPin") String tfPin, @Param("status") String status,@Param("partner_status") String partnerStatus);

    @Query(nativeQuery = true, value = "update tbl_transactions set partner_process=:status where tf_pin=:tfPin")
    @Modifying
    @Transactional
    void updatePartnerProcessStatus(@Param("tfPin") String tfPin, @Param("status") String status);
}
