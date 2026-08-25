document.addEventListener("click", (event) => {
  const action = event.target.closest('[data-action="choose-photo"]');
  if (!action) return;
  const picker = document.getElementById("capturePicker");
  if (!picker) return;
  event.preventDefault();
  event.stopImmediatePropagation();
  picker.removeAttribute("capture");
  picker.click();
}, true);
