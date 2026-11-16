package meitheal;

import meitheal.application.Clock;
import meitheal.application.MeithealService;
import meitheal.domain.catalogue.SkillCatalogue;
import meitheal.domain.model.CategoryName;
import meitheal.domain.model.Email;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Shared test fixtures. A fixed clock is used throughout so that calendar-month (R11) and 24-hour (R14)
 * boundaries are deterministic.
 */
public final class TestSupport {

    public static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");

    public static final CategoryName LEGAL = new CategoryName("Legal advice");
    public static final CategoryName FURNITURE = new CategoryName("Furniture moving");
    public static final CategoryName GARDENING = new CategoryName("Gardening");

    private TestSupport() {
    }

    public static MeithealService newService() {
        return newService(NOW);
    }

    public static MeithealService newService(Instant now) {
        SkillCatalogue catalogue = new SkillCatalogue(List.of(LEGAL, FURNITURE, GARDENING));
        return new MeithealService(catalogue, fixedClock(now));
    }

    public static Clock fixedClock(Instant now) {
        return new FixedClock(now);
    }

    public static Email email(String name) {
        return new Email(name.toLowerCase().replace(' ', '.') + "@example.com");
    }

    private record FixedClock(Instant now) implements Clock {
        @Override
        public Instant now() {
            return now;
        }

        @Override
        public LocalDate today() {
            return now.atZone(ZoneOffset.UTC).toLocalDate();
        }
    }
}
