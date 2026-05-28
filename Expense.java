import java.io.Serializable;

public class Expense implements Serializable {
    String amount, category, date, note;

    public Expense(String amount, String category, String date, String note) {
        this.amount = amount; this.category = category;
        this.date = date; this.note = note;
    }

    public String[] toRow() {
        return new String[]{date, category, "₹" + amount, note};
    }
}
