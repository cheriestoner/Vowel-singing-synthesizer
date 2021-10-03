(
Window.closeAll;
Gui_main.new;
)

(
Window.closeAll;
STD_main.new;
)

(
c = MyClass.new;
)

(
Window.closeAll;
w = Window("window", Rect(100,400,200,100))
.front()
.alwaysOnTop_(true);
f = {var view;
	view = View(w);
	view.layout = HLayout();
};
f.value;
// f.inspect;
s = NumberBox(w, 40@20)
.clipLo_(150)
.clipHi_(1000)
.action_({
	arg obj;
	var v;
	v = obj.value.postln;
});
v = View();
a = [];
10.do({a=a.add(Button(w))});
// a = { Button(w) } ! 10;
w.layout = HLayout(*a);
a.inspect;
)

(
{s = ApexSource01.ar(SinOsc.kr(6.5, 0, 0.01, 110))}.play
)