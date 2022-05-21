
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Scratch {
    //ref: https://raw.githubusercontent.com/knadh/xmlutils.py/master/samples/fruits.csv
    private static String csvdata  = """
            "id","name","price","quantity"
            "1000","Apple","4","133"
            "1001","Apricot","5","175"
            "1002","Avocado","5","182"
            "1003","Banana","5","187"
            "1004","Bilberry","5","160"
            "1005","Blackberry","4","178"
            "1006","Blackcurrant","5","102"
            "1007","Blueberry","6","156"
            "1034","Cantaloupe","4","138"
            "1008","Currant","5","194"
            "1009","Cherry","5","182"
            "1011","Clementine","3","165"
            "1013","Damson","1","164"
            "1015","Eggplant","6","189"
            "1016","Elderberry","3","189"
            "1017","Feijoa","2","198"
            "1018","Gooseberry","2","141"
            "1019","Grape","2","101"
            "1020","Grapefruit","5","199"
            "1021","Guava","1","160"
            "1023","Jackfruit","6","181"
            "1026","Kumquat","2","198"
            "1027","Legume","6","199"
            "1028","Lemon","3","120"
            "1030","Lychee","6","120"
            "1031","Mango","6","131"
            "1038","Nectarine","1","128"
            "1039","Orange","6","142"
            "1040","Peach","6","179"
            "1041","Pear","3","102"
            "1047","Pomegranate","5","112"
            "1048","Raisin","4","111"
            "1051","Rambutan","6","145"
            "1052","Redcurrant","3","190"
            "1054","Satsuma","1","197"
            "1056","Strawberry","6","178"
            "1057","Tangerine","4","119"
            "1058","Tomato","6","167"
            "1060","Watermelon","6","149"
            """;
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        CsvFile csvFile = CsvParser.parse(Arrays.stream(csvdata.split("\n")));

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        List<Fruit> fruits = new ArrayList<>();
        for (List<String> row : csvFile.lines) {
            String id = row.get(0);
            String name = row.get(1);
            String price = row.get(2);
            String quantity = row.get(3);
            System.out.println(name);

            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(new URI("https://api.dictionaryapi.dev/api/v2/entries/en/%s".formatted(name)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            String description = body.replaceAll("^.*\"partOfSpeech\"\\:\"noun\",\"definitions\"\\:\\[\\{\"definition\"\\:\"(.*?)\".*$", "$1");
            if (description.length() > 0 && !description.contains("{")) {
                System.out.println(description);
            } else {
                description = "";
            }
            BigDecimal priceDecimal = new BigDecimal(price).add(new BigDecimal(new Random().nextDouble()));
            Fruit fruit = new Fruit(new Random().nextInt(4), name, description, priceDecimal.doubleValue(), Integer.parseInt(quantity));
            fruits.add(fruit);
        }
        System.out.println(JsonSerialize.serialize(fruits));
    }


    public record CsvFile(List<String> headers, List<List<String>> lines){}
    public static class CsvParser {
        public static CsvFile parse(Stream<String> lines) {
            List<List<String>> splitLines =
                    lines.map(line -> line.split("\",\"")).map(List::of)
                            .map(elements -> elements.stream()
                                    .map(element -> element.replaceAll("^\"|\"$", "")).toList())
                            .toList();
            if (splitLines.size() < 1) {
                throw new IllegalArgumentException("No rows in CSV");
            }
            List<String> header = splitLines.get(0);
            List<List<String>> rows = splitLines.subList(1, splitLines.size());
            return new CsvFile(header, rows);
        }
    }

    public record Fruit (int supplierId, String name, String description, double price, int quantity) {}


    public static class JsonSerialize {
        public static String serialize(List<? extends Record> records) {
            StringBuilder result = new StringBuilder();
            result.append("[");
            if (records != null) {
                result.append(records.stream()
                        .map(JsonSerialize::serialize)
                        .collect(Collectors.joining(",")));
            }
            result.append("]");
            return result.toString();
        }

        private static String serialize(final Record record) {
            String fieldsJson = Arrays.stream(record.getClass().getDeclaredFields())
                    .map(field -> getFieldValue(record, field))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(","));
            return "{%s}".formatted(fieldsJson);
        }

        private static String getFieldValue(Record record, Field field) {
            try {
                Class<?> type = field.getType();
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    return null;
                }
                field.setAccessible(true);
                if (String.class.equals(type)) {
                    return "\"%s\":\"%s\"".formatted(field.getName(), field.get(record));
                } else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
                    return "\"%s\":%s".formatted(field.getName(), ((boolean)field.get(record)) ? "true": "false");
                } else if (int.class.equals(type) || Integer.class.equals(type)) {
                    return "\"%s\":%d".formatted(field.getName(), (int)field.get(record));
                } else if (long.class.equals(type) || Long.class.equals(type)) {
                    return "\"%s\":%d".formatted(field.getName(), (long)field.get(record));
                } else if (double.class.equals(type) || Double.class.equals(type)) {
                    return "\"%s\":%.2f".formatted(field.getName(), (double) field.get(record));
                } else {
                    throw new RuntimeException("Unsupported type %s attempted to serialize to JSON.".formatted(type.getName()));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unexpected error attempting to serialize to JSON for field %s.".formatted(field.getName()), e);
            }
        }
    }

}
