#!/usr/bin/env perl -W
our %m;
our $print_keys = 1;

while (<STDIN>) {
    chomp;
    s/^.*au.com.phiware.ga.Tree *- (.*)$/$1/;
    my @F = split /[+=]/;
    my ($v, $k) = split /~/, (pop @F);
    my $a = $m{$k};
    if (defined $a) {
        $m{$k} = [ $v, { $k => $a } ]
            if $a->[0] ne $v;
    } else {
        $m{$k} = [ $v, { map { reverse split /~/ } @F } ];
        my $p = $m{$k}->[1];
        foreach (keys %{$p}) {
            $p->{$_} = [$p->{$_}];
        }
    }
}

unless (@ARGV) {
    foreach (keys %m) {
        print "$_\n";
    }
}

sub print_parents {
    local %p = @_;
    local $sep = "";
    foreach (keys %p) {
        print $sep, $p{$_}->[0];
        print "~", $_ if $print_keys;
        $sep = "\t";
    }

    $sep = "\n";
    foreach (keys %p) {
        if (defined $m{$_}->[1]) {
            print $sep;
            $sep = "\t";
            print_parents(%{$m{$_}->[1]});
        }
    }
}

foreach $k (@ARGV) {
    if (defined $m{$k}->[0]) {
        print $m{$k}->[0];
        print "~", $k, "\n" if $print_keys;
        print_parents(%{$m{$k}->[1]});
        print "\n";
    }
}
