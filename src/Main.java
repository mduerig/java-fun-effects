import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Function;
import java.util.function.Supplier;

// A simple DSL
interface Console<T>{
    T accept(Visitor visitor);

    // andThen corresponds to bind / flatMap of a monad
    <S> Console<S> andThen(Function<T, Console<S>> f);
    default <S> Console<S> andThen(Supplier<Console<S>> s) {
        return andThen(__ -> s.get());
    }
}

// Different subtypes correspond to the alternatives (union type)
// of the ADT representing out DSL
class WriteLn<T> implements Console<T> {
    final String text;
    final Console<T> next;

    public WriteLn(String text, Console<T> next) {
        this.text = text;
        this.next = next;
    }

    @Override
    public T accept(Visitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public <S> Console<S> andThen(Function<T, Console<S>> f) {
        return new WriteLn<>(text, next.andThen(f));
    }
}

class ReadLn<T> implements Console<T> {
    final Function<String, Console<T>> read;

    ReadLn(Function<String, Console<T>> read) {
        this.read = read;
    }

    @Override
    public T accept(Visitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public <S> Console<S> andThen(Function<T, Console<S>> f) {
        return new ReadLn<>(s -> read.apply(s).andThen(f));
    }
}

class Result<T> implements Console<T> {
    final T result;

    Result(T result) {
        this.result = result;
    }

    @Override
    public T accept(Visitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public <S> Console<S> andThen(Function<T, Console<S>> f) {
        return f.apply(result);
    }
}

interface Visitor {
    <T> T visit(WriteLn<T> writeLn);
    <T> T visit(ReadLn<T> readLn);
    <T> T visit(Result<T> result);
}

public class Main {

    // A direct interpreter for the DSL using the visitor pattern
    // to deconstruct the ADT
    private static <T> T interp(Console<T> console) {
        return console.accept(new Visitor() {
            @Override
            public <S> S visit(WriteLn<S> writeLn) {
                System.out.println(writeLn.text);
                return writeLn.next.accept(this);
            }

            @Override
            public <S> S visit(ReadLn<S> readLn) {
                try {
                    String name = new BufferedReader(new InputStreamReader(System.in)).readLine();
                    Console<S> next = readLn.read.apply(name);
                    return next.accept(this);
                } catch (IOException e) {
                    throw new Error(e);
                }
            }

            @Override
            public <S> S visit(Result<S> result) {
                return result.result;
            }
        });
    }

    // A simple program encoded in the DSL directly
    private static final Console<Integer> program1 =
        new WriteLn<>("What's your name?",
            new ReadLn<>(name ->
                new WriteLn<>("Hello " + name,
                    new Result<>(name.length()))));

    // Convenience functions for making the DSL easier to use
    private static Console<Void> writeLn(String text) {
        return new WriteLn<>(text, new Result<>(null));
    }

    private static Console<String> readLn() {
        return new ReadLn<>(Result::new);
    }

    private static <T> Console<T> result(T t) {
        return new Result<>(t);
    }

    // Now the DSL can be used in "monadic" style
    private static final Console<Integer> program3 =
        writeLn("Say your name again:")
            .andThen(() -> readLn()
            .andThen(name -> writeLn("Hi " + name)
            .andThen(() -> result(name.length()))));

    // Composition
    private static final Console<Void> echo =
        readLn()
            .andThen(value -> writeLn(value));

    private static final Console<Void> program4 =
        writeLn("I'm your echo")
            .andThen(() -> echo);

    public static void main(String[] args) {
        System.out.println("Program 1:");
        System.out.println(interp(program1));
        System.out.println("----------");

        System.out.println("Program 3:");
        System.out.println(interp(program3));
        System.out.println("----------");

        System.out.println("Program 4: ");
        System.out.println(interp(program4));
        System.out.println("----------");
    }
}


