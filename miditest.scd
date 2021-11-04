MIDIClient.init;
MIDIClient.sources;

MIDIIn.connectAll;

MIDIdef.noteOn(\noteOnTest, {"key down".postln});

MIDIdef(\noteOnTest).free;

Synth(\default, [\freq, num.midicps, \amp, veloc * 0.00315]);


// Playing notes on your MIDI keyboard
(
var notes, on, off;

MIDIClient.init;
MIDIIn.connectAll;

notes = Array.newClear(128);    // array has one slot per possible MIDI note

on = MIDIFunc.noteOn({ |veloc, num, chan, src|

    notes[num] = Synth(\default, [\freq, num.midicps,
        \amp, veloc * 0.00315]);


});

off = MIDIFunc.noteOff({ |veloc, num, chan, src|
    notes[num].release;
});

q = { on.free; off.free; };
)

// when done:
q.value;