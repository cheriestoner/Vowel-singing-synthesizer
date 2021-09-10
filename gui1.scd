s.boot;


(
SynthDef(\svf_test, {
	arg low=0.1, band=0.0, high=0.0, notch=0.0, peak=0.0,
	f0=110.0, f1=150.0, f2=300, f3=2400, volume=4.0,
	res1=0.6, res2=0.8, res3=0.8;
	var source, sigF1, sigF2, sigF3;

	source = Blip.ar(SinOsc.kr(6.5, 0, 0.01, f0), 200, mul:volume);

	sigF1 = SVF.ar(source,
		f1, //(150,1000),    // cutoff
		res1,           // q
		low, band, high, notch, peak, mul: 4);        // levels
	sigF2 = SVF.ar(sigF1,
		f2, //(300,2000),    // cutoff
		res2,           // q
		low, band, high, notch, peak, mul: 4);        // levels
	sigF3 = SVF.ar(sigF2,
		f3,    // 2400 cutoff
		res3,           // q
		low, band, high, notch, peak, mul: 4);        // levels
	Out.ar(0, sigF3 ! 2);
}).add; //play(addAction: \addToTail);
)

// gui
(
// s.sync;

Window.closeAll;
w = Window("gui", Rect(
	Window.screenBounds.width/2-100,
	Window.screenBounds.height/2-100,
	600,
	500), scroll: true)
.front
.alwaysOnTop_(true);

w.view.decorator_(FlowLayout(w.bounds, 10@10, 10@10));

// ------------ Source ----------------

~srcView = CompositeView(w, 120@60)
.background_(Color.gray.alpha_(0.3))
.resize = 5;
~srcView.decorator_(FlowLayout(~srcView.bounds, 5@5, 5@5));

~srcLabel = StaticText(~srcView, 35@20)
.string_("Source");
~playbtn = Button(~srcView, 30@20)
.states_([
	["OFF", Color.black, Color.gray(0.8)],  // value=0
	["ON", Color.white, Color(0.2, 0.6, 1.0)]    // value=1
])
.action_({
 	arg obj;
	if(
		obj.value == 1,
		{z = Synth(\svf_test, [\low, 0.1])},
 		{z.free}
	);
});

~f0Box = NumberBox(~srcView, 40@20) // foundamental freq
.valueAction_(110)
.clipLo_(100)
.clipHi_(4000)
//.decimals_(2)
.action_({
	arg obj;
	var f0;
	f0 = obj.value.postln;
	z.set(\f0, f0);
});
~volume = Slider(~srcView, 60@20)
.action_({
	arg obj;
	var vol;
	vol = (obj.value * 10.0);
	z.set(\volume, vol);
})
.valueAction_(0.1);

// Filter type and output level
~filterView = Array.fill(2, {
	arg view;
	view = CompositeView(w, 110@60)
	.background_(Color.rand);

	view.decorator_(FlowLayout(view.bounds, 5@5));
});

~knobs = Array.fill(3, {
	Knob(~filterView[0], 30@30);
});
StaticText(~filterView[0], 30@20).string_("LP").align_(\center);
StaticText(~filterView[0], 30@20).string_("BP").align_(\center);
StaticText(~filterView[0], 30@20).string_("HP").align_(\center);
~knobs[0]
.valueAction_(0.1)
.action_({
	arg obj;
	var lv;
	lv = obj.value.postln;
	z.set(\low, lv);
});
~knobs[1]
.action_({
	arg obj;
	var lv;
	lv = obj.value.postln;
	z.set(\band, lv);
});
~knobs[2]
.action_({
	arg obj;
	var lv;
	lv = obj.value.postln;
	z.set(\high, lv);
});

StaticText(~filterView[1], 30@20).string_("Notch");
Slider(~filterView[1], 60@20)
.action_({
	arg obj;
	var lv;
	lv = obj.value;
	z.set(\notch, lv);
});
StaticText(~filterView[1], 30@20).string_("Peak");
Slider(~filterView[1], 60@20)
.action_({
	arg obj;
	var lv;
	lv = obj.value;
	z.set(\peak, lv);
});

// Formants
~fView = CompositeView(w, 240@200).background_(Color.rand);
~fView.decorator_(FlowLayout(~fView.bounds, 5@5, 5@5));
Array.fill(5, {
	arg i;
	StaticText(~fView, 40@20)
	.string_("F"++(i+1))
	.align_(\center);
});
~fFreqs = Array.fill(5, {
	arg i;
	NumberBox(~fView, 40@20)
/*	.valueAction_(150)
	.clipLo_(150)
	.clipHi_(1000)*/
	.action_({
		arg obj;
		var cutoff;
		cutoff = obj.value.postln;
		z.set("f"++(i+1), cutoff);
	});
});
~fFreqs[0].valueAction_(150).clipLo_(150).clipHi_(1000);
~fFreqs[1].valueAction_(300).clipLo_(300).clipHi_(2400);
~fFreqs[2].valueAction_(2400).clipLo_(2000).clipHi_(4000);

Array.fill(5, {
	arg i;
	StaticText(~fView, 40@20)
	.string_("Res"++(i+1))
	.align_(\center);
});
~fQ = Array.fill(5, {
	arg i;
	Knob(~fView, 40@20)
	.valueAction_(0.6)
	.action_({
		arg obj;
		var res;
		res = obj.value.postln;
		z.set("res"++(i+1), res);
	});
});

)

(
// test view and window architecture

Window.closeAll;
var window, view, view2, view3;
window = Window("gui", Rect(
	Window.screenBounds.width/2-100,
	Window.screenBounds.height/2-100,
	600,
	500), scroll: true)
.front
.alwaysOnTop_(true);

view = View()
.minSize_(Size(120, 60))
.background_(Color.gray.alpha_(0.3))
.resize_(5);

view2 = View()
.minSize_(Size(120, 60))
.background_(Color.gray.alpha_(0.8))
.resize_(5);

window.layout = VLayout.new(view, view2);
window.layout.margins_(1);

view3 = View(window, Rect(1,1,50,50));
)