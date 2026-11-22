package se.findersson.synk.form;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The weekly form: sections with fields. Stored as JSON in form_version.definition and
 * described in docs/databas.md.
 */
public record FormDefinition(List<Section> sections) {

    public record Section(String title, List<Field> fields) {}

    /**
     * @param id        key in entry.content; never reused for something else
     * @param type      one of {@link #TYPES}
     * @param label     shown above the field; may be empty
     * @param carryFrom checklists only: field whose items are copied from the previous note
     *                  when a new week is opened, or null
     */
    public record Field(String id, String type, String label, String carryFrom) {}

    public static final Set<String> TYPES = Set.of("text", "textarea", "perPerson", "checklist", "weekdays");

    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,40}");
    private static final int MAX_TEXT = 200;

    /** The definition is malformed. */
    public static class InvalidFormException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public InvalidFormException(final String message) {
            super(message);
        }
    }

    public void validate() {
        require(sections != null && !sections.isEmpty(), "formuläret saknar avsnitt");
        final Set<String> ids = new HashSet<>();
        for (final Section section : sections) {
            require(section != null && section.fields() != null, "avsnitt saknar fält");
            require(section.title() != null && !section.title().isBlank() && section.title().length() <= MAX_TEXT,
                    "avsnitt saknar rubrik");
            for (final Field field : section.fields()) {
                require(field != null && field.id() != null && ID.matcher(field.id()).matches(), "ogiltigt fält-id");
                require(ids.add(field.id()), "fält-id förekommer flera gånger: " + field.id());
                require(TYPES.contains(field.type()), "okänd fälttyp: " + field.type());
                require(field.label() != null && field.label().length() <= MAX_TEXT, "ogiltig etikett");
                require(field.carryFrom() == null || "checklist".equals(field.type()),
                        "bara checklistor kan hämta från förra veckan");
            }
        }
        for (final Field field : fields()) {
            require(field.carryFrom() == null || ids.contains(field.carryFrom()),
                    "carryFrom pekar på ett fält som inte finns: " + field.carryFrom());
        }
    }

    public List<Field> fields() {
        return sections.stream().flatMap(s -> s.fields().stream()).toList();
    }

    public boolean hasField(final String id) {
        return fields().stream().anyMatch(f -> Objects.equals(f.id(), id));
    }

    private static void require(final boolean ok, final String message) {
        if (!ok) {
            throw new InvalidFormException(message);
        }
    }

}
