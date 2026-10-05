package ec.nexo.customer.infrastructure.adapter.out.persistence.migration;

import ec.nexo.customer.infrastructure.adapter.out.persistence.crypto.FieldEncryptor;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Cifra la cédula y el teléfono que hayan quedado en claro (filas anteriores al cifrado y la semilla V100).
 * Es Java y no SQL porque necesita la clave, que nunca debe llegar a la BD. Es idempotente: salta lo ya cifrado.
 */
@Component
public class V101__EncryptCustomerPii extends BaseJavaMigration {

    private final FieldEncryptor encryptor;

    public V101__EncryptCustomerPii(FieldEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (PreparedStatement select = connection.prepareStatement("SELECT id, id_number, phone FROM customers");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE customers SET id_number = ?, phone = ? WHERE id = ?");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                String idNumber = rows.getString("id_number");
                String phone = rows.getString("phone");
                if (encryptor.isEncrypted(idNumber) && encryptor.isEncrypted(phone)) {
                    continue;
                }
                update.setString(1, encryptor.isEncrypted(idNumber) ? idNumber : encryptor.encrypt(idNumber));
                update.setString(2, encryptor.isEncrypted(phone) ? phone : encryptor.encrypt(phone));
                update.setObject(3, rows.getObject("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }
}
