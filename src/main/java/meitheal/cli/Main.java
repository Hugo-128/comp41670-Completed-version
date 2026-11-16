package meitheal.cli;

import meitheal.application.CommandResult;
import meitheal.application.MeithealService;
import meitheal.application.SystemClock;
import meitheal.domain.catalogue.SkillCatalogue;
import meitheal.domain.engagement.Engagement;
import meitheal.domain.model.CategoryName;
import meitheal.domain.model.Credits;
import meitheal.domain.model.Duration;
import meitheal.domain.model.Email;
import meitheal.domain.model.FulfilmentWindow;
import meitheal.domain.model.MemberId;
import meitheal.domain.request.Offer;
import meitheal.domain.request.Request;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * The command-line interface. A small interactive REPL exposing every operation in Brief §5.
 *
 * <p>The console adapter holds <em>no business rules</em>: it only parses input, dispatches to
 * {@link MeithealService}, and prints results. A bug here can make the system awkward to use but can never
 * make the ledger dishonest — that is the point of the layering (see Design Report §2).
 */
public final class Main {

    private static final Map<String, CategoryName> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put("legal", new CategoryName("Legal advice"));
        CATEGORIES.put("moving", new CategoryName("Furniture moving"));
        CATEGORIES.put("gardening", new CategoryName("Gardening"));
    }

    private final MeithealService service;
    private final Scanner in;
    private final Map<String, MemberId> membersByName = new LinkedHashMap<>();
    private final List<Offer> offers = new ArrayList<>();
    private final List<Request> requests = new ArrayList<>();
    private final List<Engagement> engagements = new ArrayList<>();

    public Main(MeithealService service, Scanner in) {
        this.service = service;
        this.in = in;
    }

    public static void main(String[] args) {
        MeithealService service = new MeithealService(
                new SkillCatalogue(new ArrayList<>(CATEGORIES.values())), new SystemClock());
        new Main(service, new Scanner(System.in)).run();
    }

    private void run() {
        printHelp();
        while (true) {
            System.out.print("> ");
            String line = in.hasNextLine() ? in.nextLine().trim() : "";
            if (line.isEmpty()) {
                continue;
            }
            String[] p = line.split("\\s+");
            String cmd = p[0];
            try {
                if (!dispatch(cmd, p)) {
                    return;
                }
            } catch (RuntimeException e) {
                System.out.println("error: " + e.getMessage());
            }
        }
    }

    /** @return false to quit. */
    private boolean dispatch(String cmd, String[] p) {
        switch (cmd) {
            case "register" -> {
                MemberId id = service.registerMember(p[1], new Email(p[2]));
                membersByName.put(p[1], id);
                System.out.println("registered " + id + " (" + p[1] + ") with 120-credit welcome grant");
            }
            case "suspend" -> service.suspendMember(new MemberId(p[1]));
            case "reinstate" -> service.reinstateMember(new MemberId(p[1]));
            case "offer" -> {
                Offer o = service.publishOffer(new MemberId(p[1]), category(p[2]));
                offers.add(o);
                System.out.println("offer #" + (offers.size() - 1) + " published by " + p[1]);
            }
            case "request" -> {
                FulfilmentWindow w = new FulfilmentWindow(LocalDate.parse(p[3]), LocalDate.parse(p[4]));
                Request r = service.raiseRequest(new MemberId(p[1]), category(p[2]), w);
                requests.add(r);
                System.out.println("request #" + (requests.size() - 1) + " raised by " + p[1]);
            }
            case "propose" -> {
                Offer o = offers.get(Integer.parseInt(p[1]));
                Request r = requests.get(Integer.parseInt(p[2]));
                Engagement e = service.proposeEngagement(o, r, new Duration(Integer.parseInt(p[3])),
                        LocalDate.parse(p[4]));
                engagements.add(e);
                System.out.println("engagement #" + (engagements.size() - 1) + " proposed");
            }
            case "confirm" -> {
                Engagement e = engagements.get(Integer.parseInt(p[1]));
                CommandResult r = service.confirmSession(e, new MemberId(p[2]));
                System.out.println(r.message());
            }
            case "cancel" -> {
                Engagement e = engagements.get(Integer.parseInt(p[1]));
                CommandResult r = service.cancelEngagement(e, new MemberId(p[2]));
                System.out.println(r.message());
            }
            case "grant" -> {
                CommandResult r = service.applyForGrant(new MemberId(p[1]), new Credits(Integer.parseInt(p[2])));
                System.out.println(r.message());
            }
            case "balance" -> System.out.println("balance " + p[1] + ": " + service.queryBalance(new MemberId(p[1])));
            case "ledger" -> service.queryLedger().forEach(e ->
                    System.out.println("  #" + e.sequence() + " " + e.type() + " " + e.from().label()
                            + " -> " + e.to().label() + " " + e.amount().value()));
            case "engagements" -> service.queryEngagements(new MemberId(p[1])).forEach(e ->
                    System.out.println("  " + e.provider() + " -> " + e.requester() + " " + e.hours().hours()
                            + "h " + e.state()));
            case "report" -> {
                var rep = service.report();
                System.out.println("total credits in circulation: " + rep.totalCreditsInCirculation()
                        + " (expected " + (120 * service.memberCount()) + ")");
                System.out.println("fund balance: " + rep.fundBalance());
            }
            case "help" -> printHelp();
            case "quit", "exit" -> {
                return false;
            }
            default -> System.out.println("unknown command: " + cmd + " (type help)");
        }
        return true;
    }

    private CategoryName category(String key) {
        CategoryName c = CATEGORIES.get(key);
        if (c == null) {
            throw new IllegalArgumentException("unknown category key: " + key + " (use legal/moving/gardening)");
        }
        return c;
    }

    private void printHelp() {
        System.out.println("""
                Meitheal — community time-bank. Commands:
                  register <name> <email>
                  suspend <memberId> | reinstate <memberId>
                  offer <memberId> <legal|moving|gardening>
                  request <memberId> <legal|moving|gardening> <from yyyy-MM-dd> <to yyyy-MM-dd>
                  propose <offer#idx> <request#idx> <hours> <date yyyy-MM-dd>
                  confirm <engagement#idx> <memberId> | cancel <engagement#idx> <memberId>
                  grant <memberId> <amount>
                  balance <memberId> | ledger | engagements <memberId> | report
                  help | quit
                """);
    }
}
