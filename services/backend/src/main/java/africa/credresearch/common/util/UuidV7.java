package africa.credresearch.common.util;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.util.UUID;

/** Time-ordered UUID v7 generator for primary keys (better index locality than v4). */
public final class UuidV7 {

    private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

    private UuidV7() {}

    public static UUID generate() {
        return GENERATOR.generate();
    }
}
