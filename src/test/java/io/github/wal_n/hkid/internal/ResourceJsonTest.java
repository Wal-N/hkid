package io.github.wal_n.hkid.internal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceJsonTest {
    @Test
    void rejectsJsonOutsideTheStrictStandard() {
        assertThrows(
                IllegalStateException.class,
                () -> ResourceJson.readObject(
                        "io/github/wal_n/hkid/internal/invalid-trailing-comma.json"));
    }

    @Test
    void validatesKeysAndTypesWithoutCoercion() {
        JsonObject object = JsonParser.parseString(
                "{\"schemaVersion\":1,\"entries\":[]}").getAsJsonObject();

        ResourceJson.requireExactKeys(object, "test object", "schemaVersion", "entries");
        assertEquals(1, ResourceJson.requireInteger(object, "schemaVersion", "test object"));
        assertThrows(
                IllegalStateException.class,
                () -> ResourceJson.requireExactKeys(object, "test object", "schemaVersion"));
        assertThrows(
                IllegalStateException.class,
                () -> ResourceJson.requireString(object, "schemaVersion", "test object"));
    }
}
