package utils;

import java.util.Objects;

public class TwoTuple<A, B> {
    public final A first;

    public final B second;

    public TwoTuple(A a, B b){
        first = a;
        second = b;
    }

    public String toString(){
        return "(" + first + ", " + second + ")";
    }

    public boolean equals(Object o){
        if(!(o instanceof TwoTuple)) return false;
        TwoTuple tuple=(TwoTuple) o;
        return first.equals(tuple.first) && second.equals(tuple.second);
    }

    public int hashCode(){
        return Objects.hash(first,second);
    }

}
